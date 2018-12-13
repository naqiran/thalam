package com.naqiran.thalam.configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;

import com.naqiran.thalam.annotations.AggregatingFunctors;
import com.naqiran.thalam.annotations.AggregatorLifeCycle;
import com.naqiran.thalam.annotations.LifeCyleMethodType;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceDictionaryBuilder {

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    private ServiceDictionary dictionary;

    public void build() {
        // Inject the annotated life cycle.
        final Map<String, Object> functorsMap = context.getBeansWithAnnotation(AggregatingFunctors.class);
        functorsMap.values().forEach(functor -> {
            final Method[] methods = functor.getClass().getDeclaredMethods();
            for (Method method : methods) {
                final AnnotationAttributes lifeCycleAnnotations =
                                                AnnotatedElementUtils.getMergedAnnotationAttributes(method, AggregatorLifeCycle.class);
                if (lifeCycleAnnotations != null) {
                    final String serviceId = lifeCycleAnnotations.getString("service");
                    final String version = lifeCycleAnnotations.getString("version");
                    final LifeCyleMethodType type = (LifeCyleMethodType) lifeCycleAnnotations.get("type");
                    final BaseService service = dictionary.getServiceById(serviceId, version);
                    if (service != null) {
                        if (LifeCyleMethodType.PREPARE.equals(type)) {
                            if (service instanceof Service) {
                                ((Service)service).setPrepare(getPrepareFunction(functor, method));
                            } else if (service instanceof ServiceGroup) {
                                ((ServiceGroup)service).setPrepare(getPrepareGroupFunction(functor, method));
                            }
                        }
                        else if (LifeCyleMethodType.VALIDATE.equals(type)) {
                            service.setValidate(getValidateFunction(functor, method));
                        }
                        else if (LifeCyleMethodType.MAP.equals(type)) {
                            service.setMap(getMapFunction(functor, method));
                        }
                        else if (LifeCyleMethodType.ZIP.equals(type)) {
                            service.setZip(getZipFunction(functor, method));
                        }
                    } else {
                        log.warn("Check the lifecycle method  '{}' not configured for Service Id: {} | Version: {} | Type: {}", method.toString(), serviceId, version, type);
                    }
                }
            }
        });

        injectDefaultOrConfigurableMethods();
    }

    private void injectDefaultOrConfigurableMethods() {
        if (CollectionUtils.isNotEmpty(dictionary.getServiceMap().entrySet())) {
            for (BaseService baseService : dictionary.getServiceMap().values()) {
                if (baseService instanceof Service) {
                    Service service = (Service) baseService;
                    if (CronSequenceGenerator.isValidExpression(service.getTtlExpression())) {
                        service.setTtlCron(new CronSequenceGenerator(service.getTtlExpression()));
                    }
                    if (service.getPrepare() == null) {
                        service.setPrepare(Function.identity());
                    }
                } else if (baseService instanceof ServiceGroup) {
                    ServiceGroup group = (ServiceGroup) baseService;
                    
                    if (group.getPrepare() == null && StringUtils.isNotBlank(group.getForkAttribute())) {
                        group.setPrepare(request -> {
                            String parameter = request.getParameters().get(group.getForkAttribute());
                            if (StringUtils.isNotBlank(parameter)) {
                                return Stream.of(parameter.split(",")).map(splitParam -> {
                                    final ServiceRequest clonedRequest = CoreUtils.cloneServiceRequestForServiceGroup(group, request, null);
                                    clonedRequest.getParameters().put(group.getForkAttribute(), splitParam);
                                    return clonedRequest;
                                });
                            }
                            return Stream.of(request);
                        });
                    }
                    if (group.getPrepare() == null) {
                        group.setPrepare(request -> Stream.of(request));
                    }
                }

                if (baseService.getValidate() == null) {
                    baseService.setValidate(getDefaultValidateMethod(baseService));
                }

                if (baseService.getMap() == null) {
                    baseService.setMap(Function.identity());
                }

                if (baseService.getZip() == null) {
                    baseService.setZip(getDefaultZipMethod(baseService));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(dictionary.getServiceGroups())) {
            for (ServiceGroup group : dictionary.getServiceGroups()) {
                // Replace with the actual instance of the service.
                if (group.getService() != null) {
                    group.setService(getServiceById(group.getService()));
                }
                if (group.getServices() != null) {
                    group.setServices(group.getServices().stream().map(svc -> getServiceById(svc)).collect(Collectors.toList()));
                }
            }
        }
    }

    private BaseService getServiceById(final BaseService tempService) {
        return Optional.ofNullable(dictionary.getServiceById(tempService.getId(), tempService.getVersion())).orElseThrow(
                                        () -> new IllegalStateException("No Service Exist with the id: " + tempService.getId()));
    }

    private final Function<ServiceRequest, ServiceRequest> getPrepareFunction(final Object functor, final Method method) {
        final String message = "Prepare method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) :"
                                        + method.getName();
        CoreUtils.validateLifeCycleFunction(method, message, ServiceRequest.class, ServiceRequest.class);
        return (request) -> {
            try {
                return (ServiceRequest) method.invoke(functor, request);
            } catch (final Exception e) {
                log.error("Error in preparing the request {} : {}", request.getService().getId(), e);
            }
            return request;
        };
    }

    @SuppressWarnings("unchecked")
    private final Function<ServiceRequest, Stream<ServiceRequest>> getPrepareGroupFunction(final Object functor, final Method method) {
        final String message = "Prepare method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) :"
                                        + method.getName();
        // Assert.state(method.getReturnType().isAssignableFrom(ServiceRequest.class), message);
        final Parameter[] parameters = method.getParameters();
        if (parameters != null) {
            Assert.state(parameters.length == 1, message);
            for (final Parameter parameter : parameters) {
                Assert.state(parameter.getType().equals(ServiceRequest.class), message);
            }
        }
        return (request) -> {
            try {
                return (Stream<ServiceRequest>) method.invoke(functor, request);
            } catch (final Exception e) {
                log.error("Error in preparing the request {} : {}", request.getService().getId(), e);
            }
            return Stream.of(request);
        };
    }

    private final Function<ServiceRequest, Boolean> getValidateFunction(final Object functor, final Method method) {
        final String message = "Validate method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) : "
                                        + method.getName();
        CoreUtils.validateLifeCycleFunction(method, message, Boolean.class, ServiceRequest.class);
        return (request) -> {
            try {
                return (Boolean) method.invoke(functor, request);
            } catch (final Exception e) {
                log.error("Error in validating the request {} : {}", request.getService().getId(), e);
            }
            return Boolean.FALSE;
        };
    }

    private Function<ServiceRequest, Boolean> getDefaultValidateMethod(final BaseService service) {
        if (StringUtils.isNotBlank(service.getPreValidateExpression())) {
            return (request) -> {
                return Boolean.TRUE.equals(CoreUtils.evaluateSPEL(service.getPreValidateExpression(), request, Boolean.class));
            };
        }
        return (request) -> Boolean.TRUE;
    }

    private final Function<ServiceResponse, ServiceResponse> getMapFunction(final Object functor, final Method method) {
        final String message = "Map method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) : "
                                        + method.getName();
        CoreUtils.validateLifeCycleFunction(method, message, ServiceResponse.class, ServiceResponse.class);
        return (response) -> {
            try {
                return (ServiceResponse) method.invoke(functor, response);
            } catch (final Exception e) {
                log.error("Error in Mapping the response {}", e);
            }
            return response;
        };
    }

    private BiFunction<ServiceResponse, ServiceResponse, ServiceResponse> getZipFunction(Object functor, Method method) {
        final String message = "Zip method should be of following signature 'ServiceResponse methodName(ServiceResponse, ServiceResponse) :"
                                        + method.getName();
        CoreUtils.validateLifeCycleFunction(method, message, ServiceResponse.class, ServiceResponse.class, ServiceResponse.class);
        return (sourceResponse, targetResponse) -> {
            try {
                final ServiceResponse mergedDefaultResponse = mergeResponse(sourceResponse, targetResponse);
                final ServiceResponse mergedResponse = (ServiceResponse) method.invoke(functor, sourceResponse, targetResponse);
                mergedDefaultResponse.setValue(mergedResponse.getValue());
            } catch (final Exception e) {
                log.error("Error in Mapping the response {}", e);
            }
            return sourceResponse;
        };
    }

    @SuppressWarnings("unchecked")
    private BiFunction<ServiceResponse, ServiceResponse, ServiceResponse> getDefaultZipMethod(final BaseService service) {
        return (sourceResponse, targetResponse) -> {
            final ExpressionParser parser = new SpelExpressionParser();
            final Expression targetExpression = service.getTargetExpression() != null ? parser.parseExpression(service.getTargetExpression()) : null;
            final ServiceResponse mergedResponse = mergeResponse(sourceResponse, targetResponse);
            if (sourceResponse != null && targetResponse != null) {
                try {
                    if (StringUtils.isBlank(service.getSourceExpression()) && StringUtils.isBlank(service.getTargetExpression())) {
                        log.warn("No Zipping Information: {}", service.getId());
                    }
                    Object targetValue = targetResponse.getValue();
                    if (targetExpression != null && targetResponse.getValue() != null) {
                        targetValue = targetExpression.getValue(targetResponse.getValue());
                    }
                    if (StringUtils.isNotBlank(service.getSourceExpression()) && sourceResponse.getValue() != null) {
                        if (sourceResponse.getValue() instanceof Map) {
                            ((Map<String,Object>)sourceResponse.getValue()).put(service.getSourceExpression(), targetValue);
                        } else {
                            final Expression sourceExpression = service.getSourceExpression() != null ? parser.parseExpression(service.getSourceExpression()) : null;
                            sourceExpression.setValue(sourceResponse.getValue(), targetValue);
                        }
                        mergedResponse.setValue(sourceResponse.getValue());
                    } else if (targetResponse.getValue() != null) {
                        mergedResponse.setValue(targetResponse.getValue());
                    } else if (sourceResponse.getValue() != null) {
                        mergedResponse.setValue(sourceResponse.getValue());
                    }
                } catch (Exception e) {
                    ServiceMessage serviceMessage = ServiceMessage.builder().message("Exception in Zipping: " + e.getMessage()).exception(e).build();
                    mergedResponse.addMessage(serviceMessage);
                }
            } 
            return mergedResponse;
        };
    }

    private ServiceResponse mergeResponse(final ServiceResponse sourceResponse, final ServiceResponse targetResponse) {
        final ServiceResponse zippedResponse = CoreUtils.createServiceResponse(ThalamConstants.ZIP_DEFAULT_SOURCE, null);
        zippedResponse.setMessages(new ArrayList<>());
        if (CollectionUtils.isNotEmpty(sourceResponse.getMessages()) && !ThalamConstants.ZIP_DUMMY_SOURCE.equals(sourceResponse.getSource())) {
            zippedResponse.getMessages().addAll(sourceResponse.getMessages());
        }
        if (CollectionUtils.isNotEmpty(targetResponse.getMessages())) {
            zippedResponse.getMessages().addAll(targetResponse.getMessages());
        }
        return zippedResponse;
    }
}

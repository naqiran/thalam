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
                    final Service service = dictionary.getServiceById(serviceId, version);
                    final ServiceGroup serviceGroup = dictionary.getServiceGroupById(serviceId, version);
                    if (service != null) {
                        if (LifeCyleMethodType.PREPARE.equals(type)) {
                            service.setPrepare(getPrepareFunction(functor, method));
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
                    }
                    else {
                        if (serviceGroup != null) {
                            if (LifeCyleMethodType.PREPARE.equals(type)) {
                                serviceGroup.setPrepare(getPrepareGroupFunction(functor, method));
                            }
                        }
                        log.warn("Check the lifecycle method not configured {} : {} : {}", serviceId, version, type);
                    }
                }
            }
        });

        injectDefaultOrConfigurableMethods();
    }

    private void injectDefaultOrConfigurableMethods() {
        if (CollectionUtils.isNotEmpty(dictionary.getServices())) {
            for (Service service : dictionary.getServices()) {

                if (CronSequenceGenerator.isValidExpression(service.getTtlExpression())) {
                    service.setTtlCron(new CronSequenceGenerator(service.getTtlExpression()));
                }

                if (service.getPrepare() == null) {
                    service.setPrepare(Function.identity());
                }

                if (service.getValidate() == null) {
                    service.setValidate(getDefaultValidateMethod(service));
                }

                if (service.getMap() == null) {
                    service.setMap(Function.identity());
                }

                if (service.getZip() == null) {
                    service.setZip(getDefaultZipMethod(service));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(dictionary.getServiceGroups())) {
            for (ServiceGroup group : dictionary.getServiceGroups()) {
                if (group.getPrepare() == null && StringUtils.isNotBlank(group.getForkAttribute())) {
                    group.setPrepare(request -> {
                        String parameter = request.getParameters().get(group.getForkAttribute());
                        if (StringUtils.isNotBlank(parameter)) {
                            return Stream.of(parameter.split(",")).map(splitParam -> {
                                final ServiceRequest clonedRequest = CoreUtils.cloneServiceRequestForServiceGroup(group, request);
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

                // Replace with the actual instance of the service.
                if (group.getService() != null) {
                    group.setService(getServiceById(group.getService()));
                }
                if (group.getServices() != null) {
                    group.setServices(group.getServices().stream().map(svc -> getServiceById(svc)).collect(Collectors.toList()));
                }
                if (group.getServiceGroup() != null) {
                    group.setServiceGroup(getServiceGroupById(group.getServiceGroup()));
                }
                if (group.getServiceGroups() != null) {
                    group.setServiceGroups(group.getServiceGroups().stream().map(svc -> getServiceGroupById(svc)).collect(Collectors.toList()));
                }
            }
        }
    }

    private Service getServiceById(final Service tempService) {
        return Optional.ofNullable(dictionary.getServiceById(tempService.getId(), tempService.getVersion())).orElseThrow(
                                        () -> new IllegalStateException("No Service Exist with the id: " + tempService.getId()));
    }

    private ServiceGroup getServiceGroupById(final ServiceGroup tempServiceGroup) {
        return Optional.ofNullable(dictionary.getServiceGroupById(tempServiceGroup.getId(), tempServiceGroup.getVersion())).orElseThrow(
                                        () -> new IllegalStateException("No Service Group Exist with the id: " + tempServiceGroup.getId()));
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

    private Function<ServiceRequest, Boolean> getDefaultValidateMethod(final Service service) {
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
        return (response1, response2) -> {
            try {
                final ServiceResponse mergedDefaultResponse = mergeResponse(response1, response2);
                final ServiceResponse mergedResponse = (ServiceResponse) method.invoke(functor, response1, response2);
                mergedDefaultResponse.setValue(mergedResponse.getValue());
            } catch (final Exception e) {
                log.error("Error in Mapping the response {}", e);
            }
            return response1;
        };
    }

    @SuppressWarnings("unchecked")
    private BiFunction<ServiceResponse, ServiceResponse, ServiceResponse> getDefaultZipMethod(final Service service) {
        return (sourceResponse, targetResponse) -> {
            final ExpressionParser parser = new SpelExpressionParser();
            final Expression targetExpression = service.getTargetExpression() != null ? parser.parseExpression(service.getTargetExpression()) : null;
            final ServiceResponse mergedResponse = mergeResponse(sourceResponse, targetResponse);
            if (sourceResponse != null && targetResponse != null && sourceResponse.getValue() != null && targetResponse.getValue() != null) {
                try {
                    Object targetValue = targetResponse.getValue();
                    if (targetExpression != null) {
                        targetValue = targetExpression.getValue(targetResponse.getValue());
                    }
                    if (StringUtils.isNotBlank(service.getSourceExpression())) {
                        if (sourceResponse.getValue() instanceof Map) {
                            ((Map<String,Object>)sourceResponse.getValue()).put(service.getSourceExpression(), targetValue);
                        } else {
                            final Expression sourceExpression = service.getSourceExpression() != null ? parser.parseExpression(service.getSourceExpression()) : null;
                            sourceExpression.setValue(sourceResponse.getValue(), targetValue);
                        }
                    }
                    mergedResponse.setValue(sourceResponse.getValue());
                } catch (Exception e) {
                    log.error("Exception in Zipping the Service", e);
                    ServiceMessage serviceMessage = ServiceMessage.builder().message("Exception in Zipping: " + e.getMessage()).exception(e).build();
                    mergedResponse.addMessage(serviceMessage);
                }
            } else {
                mergedResponse.setValue(targetResponse.getValue());
            }
            return mergedResponse;
        };
    }

    private ServiceResponse mergeResponse(final ServiceResponse resp1, final ServiceResponse resp2) {
        final ServiceResponse zippedResponse = CoreUtils.createServiceResponse(ThalamConstants.ZIP_DEFAULT_SOURCE, null);
        zippedResponse.setMessages(new ArrayList<>());
        if (CollectionUtils.isNotEmpty(resp1.getMessages()) && !ThalamConstants.ZIP_DUMMY_SOURCE.equals(resp1.getSource())) {
            zippedResponse.getMessages().addAll(resp1.getMessages());
        }
        if (CollectionUtils.isNotEmpty(resp2.getMessages())) {
            zippedResponse.getMessages().addAll(resp2.getMessages());
        }
        return zippedResponse;
    }
}

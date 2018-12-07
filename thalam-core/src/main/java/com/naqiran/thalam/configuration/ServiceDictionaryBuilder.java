package com.naqiran.thalam.configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;

import com.naqiran.thalam.annotations.AggregatingFunctors;
import com.naqiran.thalam.annotations.AggregatorLifeCycle;
import com.naqiran.thalam.annotations.LifeCyleMethodType;
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
        final Map<String,Object> functorsMap = context.getBeansWithAnnotation(AggregatingFunctors.class);
        functorsMap.values().forEach(functor -> {
            final Method[] methods = functor.getClass().getDeclaredMethods();
            for (Method method: methods) {
                final AnnotationAttributes lifeCycleAnnotations = AnnotatedElementUtils.getMergedAnnotationAttributes(method, AggregatorLifeCycle.class);
                if (lifeCycleAnnotations != null) {
                    final String serviceId =  lifeCycleAnnotations.getString("service");
                    final String version = lifeCycleAnnotations.getString("version");
                    final LifeCyleMethodType type = (LifeCyleMethodType) lifeCycleAnnotations.get("type");
                    final Service service = dictionary.getServiceById(serviceId, version);
                    final ServiceGroup serviceGroup = dictionary.getServiceGroupById(serviceId, version);
                    if (service != null) {
                        if (LifeCyleMethodType.PREPARE.equals(type)) {
                            service.setPrepare(getPrepareFunction(functor, method));
                        } else if (LifeCyleMethodType.VALIDATE.equals(type)){
                            service.setValidate(getValidateFunction(functor, method));
                        } else if (LifeCyleMethodType.MAP.equals(type)){
                            service.setMap(getMapFunction(functor, method));
                        }
                    } else {
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
                    if (StringUtils.isNotBlank(service.getPreValidateExpression())) {
                        service.setValidate((request) -> {
                            return Boolean.TRUE.equals(CoreUtils.evaluateSPEL(service.getPreValidateExpression(), request, Boolean.class));
                        });
                    } else {
                        service.setValidate((request) -> Boolean.TRUE);
                    }
                }
                
                if (service.getMap() == null) {
                    service.setMap(Function.identity());
                }
            }
        }
        
        if (CollectionUtils.isNotEmpty(dictionary.getServiceGroups())) {
            for (ServiceGroup group: dictionary.getServiceGroups()) {
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
                if (group.getService() != null) {
                    final Service service = dictionary.getServiceById(group.getService().getId(), group.getService().getVersion());
                    Assert.notNull(service, "No Service Exist with the id: " + group.getService().getId());
                    if (service  != null) {
                        group.setService(service);
                    }
                }
                if (group.getServiceGroup() != null) {
                    final ServiceGroup serviceGroup = dictionary.getServiceGroupById(group.getServiceGroup().getId(), group.getServiceGroup().getVersion());
                    Assert.notNull(serviceGroup, "No Service Group Exist with the id: " + group.getServiceGroup().getId());
                    if (serviceGroup  != null) {
                        group.setServiceGroup(serviceGroup);
                    }
                }
            }
        }
    }
    
    private final Function<ServiceRequest,ServiceRequest> getPrepareFunction(final Object functor, final Method method) {
        final String message = "Prepare method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) :" + method.getName();
        validateLifeCycleFunction(method, message, ServiceRequest.class, ServiceRequest.class);
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
    private final Function<ServiceRequest,Stream<ServiceRequest>> getPrepareGroupFunction(final Object functor, final Method method) {
        final String message = "Prepare method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) :" + method.getName();
        //Assert.state(method.getReturnType().isAssignableFrom(ServiceRequest.class), message);
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
    
    private final Function<ServiceRequest,Boolean> getValidateFunction(final Object functor, final Method method) {
        final String message = "Validate method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) : " + method.getName();
        validateLifeCycleFunction(method, message, Boolean.class, ServiceRequest.class);
        return (request) -> {
            try {
                return (Boolean) method.invoke(functor, request);
            } catch (final Exception e) {
                log.error("Error in validating the request {} : {}", request.getService().getId(), e);
            } 
            return Boolean.FALSE;
        };
    }
    
    private final Function<ServiceResponse,ServiceResponse> getMapFunction(final Object functor, final Method method) {
        final String message = "Map method should be of following signature 'ServiceRequest methodName(final ServiceRequest request) : " + method.getName();
        validateLifeCycleFunction(method, message, ServiceResponse.class, ServiceResponse.class);
        return (response) -> {
            try {
                return (ServiceResponse) method.invoke(functor, response);
            } catch (final Exception e) {
                log.error("Error in Mapping the response {}",  e);
            } 
            return response;
        };
    }
    
    private final void validateLifeCycleFunction(final Method method, final String message, final Class<?> responseType, Class<?>... methodParameters) {
        Assert.state(method.getReturnType().isAssignableFrom(responseType), message);
        final Parameter[] parameters = method.getParameters();
        if (parameters != null && methodParameters != null) {
            Assert.state(parameters.length == methodParameters.length, message);
            for (int i=0; i < parameters.length; i++) {
                Assert.state(parameters[i].getType().equals(methodParameters[i]), message);
            }
        }
    }
    
}

package com.naqiran.thalam.configuration;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;

import com.naqiran.thalam.annotations.AggregatingFunctors;
import com.naqiran.thalam.annotations.AggregatorLifeCycle;
import com.naqiran.thalam.annotations.LifeCyleMethodType;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceDictionaryBuilder {
    
    @Autowired
    ConfigurableApplicationContext context;
    
    @Autowired
    private ServiceDictionary dictionary;
    
    public void build() {
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
                    if (service != null) {
                        if (LifeCyleMethodType.PREPARE.equals(type)) {
                            service.setPrepare(getPrepareFunction(functor, method));
                        } else if (LifeCyleMethodType.VALIDATE.equals(type)){
                            service.setValidate(getValidateFunction(functor, method));
                        }
                    } else {
                        log.warn("Check the lifecycle method not configured {} : {} : {}", serviceId, version, type);
                    }
                }
            }
        });
        
        if (CollectionUtils.isNotEmpty(dictionary.getServices())) {
            for (Service service : dictionary.getServices()) {
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
                
            }
        }
    }
    
    private final Function<ServiceRequest,ServiceRequest> getPrepareFunction(final Object functor, final Method method) {
        Assert.state(method.getReturnType().isAssignableFrom(ServiceRequest.class), "This is not a valid validate function");
        return (request) -> {
            try {
                return (ServiceRequest) method.invoke(functor, request);
            } catch (final Exception e) {
                log.error("Error in preparing the request {} : {}", request.getService().getId(), e);
            } 
            return request;
        };
    }
    
    private final Function<ServiceRequest,Boolean> getValidateFunction(final Object functor, final Method method) {
        Assert.state(method.getReturnType().isAssignableFrom(Boolean.class), "Validate function should return Boolean: " + method.getName());
        return (request) -> {
            try {
                return (Boolean) method.invoke(functor, request);
            } catch (final Exception e) {
                log.error("Error in validating the request {} : {}", request.getService().getId(), e);
            } 
            return Boolean.FALSE;
        };
    }
    
}

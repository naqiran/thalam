package com.naqiran.thalam.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class ServiceDictionaryValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(Service.class) || clazz.isAssignableFrom(ServiceGroup.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof Service) {
            validateService((Service) target, errors);
        }
    }

    public void validateService(Service service, Errors errors) {
        if (service.isCacheEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "cacheName", "service.cache.name.empty", "Cache Name should not be empty");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "cacheKeyFormat", "service.cache.keyformat.empty",
                                            "Cache Key format should not be empty when cache is enabled");
        }

        if (StringUtils.isBlank(service.getBaseUrl())) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "discoveryId", "service.discovery.id.empty",
                                            "Either the discovery id or base url should be present for service: {}");
        }

        if (StringUtils.isBlank(service.getDiscoveryId())) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "baseUrl", "service.discovery.id.empty",
                                            "Either the discovery id or base url should be present for service: {}");
        }

        if (service.getTtl() != null && StringUtils.isNotBlank(service.getTtlExpression())) {
            errors.reject("service.ttl.expression.error", "Either the ttl or ttl expression should be set for the service");
        }
    }

    public void validateServiceGroup(ServiceGroup target, Errors errors) {
        if (ExecutionType.FORK.equals(target.getExecutionType())) {
            if (target.getService() == null && target.getServiceGroup() == null) {
                errors.reject("servicegroup.empty.fork", "Either Service or Service Group should be configured for forking");
            }
        }
    }

}

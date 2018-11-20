package com.naqiran.thalam.service.model;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("serial")
public class ServiceException extends RuntimeException {
    private String message;
    private final Throwable wrappedException;
    
    public ServiceException(final String message) {
        super();
        this.message = message;
        this.wrappedException = null;
    }
    
    public ServiceException(final String message, final Throwable exception) {
        super();
        this.message = message;
        this.wrappedException = exception;
    }
    
    @Override
    public String getMessage() {
        if (wrappedException != null && StringUtils.isNotBlank(wrappedException.getMessage())) {
            return StringUtils.defaultString(message) + wrappedException.getMessage();
        }
        return StringUtils.defaultString(message);
    }
}

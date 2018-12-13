package com.naqiran.thalam.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified Message for Thalam (Platform)
 * @author Nakkeeran Annamalai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = "exception")
public class ServiceMessage {
    private String id;
    private String message;
    private Object messageBody;
    @Builder.Default
    private ServiceMessageType type = ServiceMessageType.INFO;
    private Throwable exception;
}

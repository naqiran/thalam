package com.naqiran.thalam.service.model;

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
public class ServiceMessage {
    private String id;
    private String message;
    @Builder.Default
    private ServiceMessageType type = ServiceMessageType.INFO;
    private Throwable exception;
}

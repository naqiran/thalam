package com.naqiran.thalam.service.model;

import lombok.Data;

/**
 * Unified Message for Thalam (Platform)
 * @author Nakkeeran Annamalai
 */
@Data
public class ServiceMessage {
    private String message;
    private ServiceMessageType type = ServiceMessageType.INFO;
}

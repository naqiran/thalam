package com.naqiran.thalam.service.model;

import lombok.Data;

/**
 * @author Nakkeeran Annamalai
 */
@Data
public class ServiceMessage {
    private String message;
    private ServiceMessageType type = ServiceMessageType.INFO;
}

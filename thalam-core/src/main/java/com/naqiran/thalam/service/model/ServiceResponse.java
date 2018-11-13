package com.naqiran.thalam.service.model;

import java.util.List;

import lombok.Data;

/**
 * @author Nakkeeran Annamalai
 */
@Data
public class ServiceResponse {
    private Object response;
    private List<ServiceMessage> messages;
}

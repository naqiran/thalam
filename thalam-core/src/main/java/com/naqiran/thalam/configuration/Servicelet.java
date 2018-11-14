package com.naqiran.thalam.configuration;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Servicelet Description
 * @author Nakkeeran Annamalai
 * 
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Servicelet extends Service {
    List<Service> services;
}
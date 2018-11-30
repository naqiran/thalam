package com.naqiran.thalam.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Attribute {
    private String name;
    private String value;
    private AttributeType type = AttributeType.DEFAULT;
}

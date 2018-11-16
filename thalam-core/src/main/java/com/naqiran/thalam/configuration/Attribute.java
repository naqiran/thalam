package com.naqiran.thalam.configuration;

import lombok.Data;

@Data
public class Attribute {
    private String name;
    private String value;
    private AttributeType type = AttributeType.DEFAULT;
}

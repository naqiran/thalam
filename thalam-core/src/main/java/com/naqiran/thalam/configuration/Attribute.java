package com.naqiran.thalam.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Attribute {
    private String name;
    private String value;
    private AttributeType type = AttributeType.DEFAULT;
}

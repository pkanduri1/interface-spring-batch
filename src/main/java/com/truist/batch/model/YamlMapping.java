package com.truist.batch.model;

import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class YamlMapping{

    private String fileType;
    private String transactionType;
    private Map<String,FieldMapping> fields;
}
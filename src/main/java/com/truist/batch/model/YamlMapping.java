package com.truist.batch.model;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YamlMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fileType;
    private String transactionType;
    private Map<String, FieldMapping> fields;
}
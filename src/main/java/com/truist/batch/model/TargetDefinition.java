package com.truist.batch.model;

import java.util.List;

import lombok.Data;

/**
 * Configuration for a specific batch job under a source system.
 */
@Data
public class TargetDefinition {
    private String targetName;
    private String description;
    private String version;
    private FileType fileType;
    private Integer recordLength;
    private List<String> transactionTypes;
    private List<TargetField> fields;
    
    public TargetField getField(String fieldName) {
        return fields.stream()
            .filter(f -> f.getName().equals(fieldName))
            .findFirst()
            .orElse(null);
    }
}
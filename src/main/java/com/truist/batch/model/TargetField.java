package com.truist.batch.model;

import lombok.Data;

/**
 * Configuration for a specific batch job under a source system.
 */
@Data
public class TargetField {
    private String name;
    private Integer position;
    private Integer length;
    private DataType dataType;
    private String format;
    private PaddingConfig padding;
    private String defaultValue;
    private FieldValidation validation;
    private String description;
}
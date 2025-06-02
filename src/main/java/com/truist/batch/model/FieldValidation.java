package com.truist.batch.model;

import java.util.List;

import lombok.Data;

@Data
public class FieldValidation {
    private boolean required;
    private String pattern;
    private Integer maxLength;
    private List<String> allowedValues;
}

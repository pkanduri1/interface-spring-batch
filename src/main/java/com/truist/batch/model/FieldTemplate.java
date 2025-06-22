package com.truist.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldTemplate {
    private String fileType;
    private String transactionType;
    private String fieldName;
    private Integer targetPosition;  // Sequential: 1, 2, 3, 4...
    private Integer length;
    private String dataType;
    private String format;
    private String required;
    private String description;
    private Integer version;
    private String enabled;
}

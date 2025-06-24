package com.truist.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceField {
    private String name;
    private String dataType; // "STRING", "NUMBER", "DATE", "BOOLEAN"
    private String description;
    private boolean nullable;
    private Integer maxLength;
    private String sourceTable;
    private String sourceColumn;
}
package com.truist.batch.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class SourceField {
    private String name;
    private String type; // "STRING", "NUMBER", "DATE", "BOOLEAN"
    private String description;
    private boolean nullable;
    private Integer maxLength;
    private String sourceTable;
    private String sourceColumn;
}
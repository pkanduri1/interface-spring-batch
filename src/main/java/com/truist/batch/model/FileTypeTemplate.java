package com.truist.batch.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeTemplate {
    private String fileType;
    private String description;
    private Integer totalFields;
    private Integer recordLength;
    private String enabled;
    private List<FieldTemplate> fields;
}
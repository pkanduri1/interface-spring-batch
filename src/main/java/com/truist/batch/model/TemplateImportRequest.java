package com.truist.batch.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateImportRequest {
    private String fileType;
    private String description;
    private String createdBy;
    private boolean replaceExisting;
    private List<FieldTemplate> fields;
}

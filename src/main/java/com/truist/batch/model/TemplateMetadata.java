package com.truist.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMetadata {
    private String fileType;
    private String transactionType;
    private Integer templateVersion;
    private Integer fieldsFromTemplate;
    private Integer totalFields;
    private String generatedAt;
    private String generatedBy;
}

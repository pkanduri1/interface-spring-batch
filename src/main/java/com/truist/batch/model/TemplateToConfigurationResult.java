package com.truist.batch.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateToConfigurationResult {
    private String sourceSystem;
    private String jobName;
    private String transactionType;
    private String description;
    private List<FieldMapping> fields;
    private LocalDateTime createdDate;
    private String createdBy;
    private Integer version;
    private TemplateMetadata templateMetadata;
    
    // Constructor from FieldMappingConfig
    public static TemplateToConfigurationResult fromFieldMappingConfig(
            FieldMappingConfig config, TemplateMetadata metadata) {
        TemplateToConfigurationResult result = new TemplateToConfigurationResult();
        result.setSourceSystem(config.getSourceSystem());
        result.setJobName(config.getJobName());
        result.setTransactionType(config.getTransactionType());
        result.setDescription(config.getDescription());
        result.setFields(config.getFieldMappings());
        result.setCreatedDate(config.getLastModified());
        result.setCreatedBy(config.getModifiedBy());
        result.setVersion(config.getVersion());
        result.setTemplateMetadata(metadata);
        return result;
    }
}

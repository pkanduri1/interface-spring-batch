package com.truist.batch.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingConfig {
    private String sourceSystem;
    private String jobName;
    private String transactionType;
    private String description;
    private List<FieldMapping> fieldMappings;
    private LocalDateTime lastModified;
    private String modifiedBy;
    private int version;
}
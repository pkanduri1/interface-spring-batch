package com.truist.batch.service;

import java.util.List;
import java.util.Map;

import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.JobConfig;
import com.truist.batch.model.SourceField;
import com.truist.batch.model.SourceSystem;
import com.truist.batch.model.TestResult;
import com.truist.batch.model.ValidationResult;

public interface ConfigurationService {
    
    // Source Systems
    List<SourceSystem> getAllSourceSystems();
    SourceSystem getSourceSystem(String systemId);
    List<JobConfig> getJobsForSystem(String systemId);
    List<SourceField> getSourceFields(String systemId, String jobName);
    
    // Field Mappings
    FieldMappingConfig getFieldMappings(String sourceSystem, String jobName);
    FieldMappingConfig getFieldMappings(String sourceSystem, String jobName, String transactionType);
    String saveConfiguration(FieldMappingConfig config);
    
    // Validation & Generation
    ValidationResult validateConfiguration(FieldMappingConfig config);
    String generateYaml(FieldMappingConfig config);
    List<String> generatePreview(FieldMappingConfig mapping, List<Map<String, Object>> sampleData);
    TestResult testConfiguration(String sourceSystem, String jobName);
}

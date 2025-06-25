package com.truist.batch.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.FieldTemplate;
import com.truist.batch.model.FileTypeTemplate;
import com.truist.batch.model.TemplateImportRequest;
import com.truist.batch.model.TemplateImportResult;
import com.truist.batch.model.ValidationResult;

public interface TemplateService {
    
    // File Type Template operations
    List<FileTypeTemplate> getAllFileTypes();
    Optional<FileTypeTemplate> getFileTypeTemplate(String fileType);
    FileTypeTemplate createFileTypeTemplate(FileTypeTemplate template, String createdBy);
    FileTypeTemplate updateFileTypeTemplate(FileTypeTemplate template, String modifiedBy);
    void deleteFileTypeTemplate(String fileType, String deletedBy);
    
    // Field Template operations
    List<FieldTemplate> getFieldTemplatesByFileTypeAndTransactionType(String fileType, String transactionType);
    Optional<FieldTemplate> getFieldTemplate(String fileType, String transactionType, String fieldName);
    FieldTemplate createFieldTemplate(FieldTemplate template, String createdBy);
    FieldTemplate updateFieldTemplate(FieldTemplate template, String modifiedBy);
    void deleteFieldTemplate(String fileType, String transactionType, String fieldName, String deletedBy);
    
    // Transaction type operations
    List<String> getTransactionTypesByFileType(String fileType);
    
    // Import operations
    TemplateImportResult importFromExcel(MultipartFile file, String fileType, String createdBy);
    TemplateImportResult importFromJson(TemplateImportRequest request);
    
    // Validation operations
    ValidationResult validateTemplate(FileTypeTemplate template);
    ValidationResult validateFieldTemplate(FieldTemplate field);
    
    // Configuration integration
    FieldMappingConfig createConfigurationFromTemplate(
        String fileType, String transactionType, String sourceSystem, String jobName, String createdBy);
 // Add these methods to the TemplateService interface

    /**
     * Field template CRUD operations
     */
    FieldTemplate createFieldTemplate(FieldTemplate fieldTemplate);
    FieldTemplate updateFieldTemplate(FieldTemplate fieldTemplate);
    FieldTemplate duplicateFieldTemplate(String fileType, String transactionType, String fieldName, 
                                        String newFieldName, Integer newPosition, String createdBy);

    /**
     * Bulk operations
     */
    List<FieldTemplate> bulkUpdateFieldTemplates(String fileType, List<FieldTemplate> fields);
    List<FieldTemplate> reorderFieldTemplates(String fileType, List<Map<String, Object>> fieldOrders, String modifiedBy);

    /**
     * Validation
     */
    ValidationResult validateFieldTemplates(String fileType, List<FieldTemplate> fields);
}


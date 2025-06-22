package com.truist.batch.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.truist.batch.entity.FieldTemplateEntity;
import com.truist.batch.entity.FieldTemplateId;
import com.truist.batch.entity.FileTypeTemplateEntity;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.FieldTemplate;
import com.truist.batch.model.FileTypeTemplate;
import com.truist.batch.model.TemplateImportRequest;
import com.truist.batch.model.TemplateImportResult;
import com.truist.batch.model.ValidationResult;
import com.truist.batch.repository.FieldTemplateRepository;
import com.truist.batch.repository.FileTypeTemplateRepository;
import com.truist.batch.service.AuditService;
import com.truist.batch.service.TemplateService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private FieldTemplateRepository fieldTemplateRepository;
    
    @Autowired
    private FileTypeTemplateRepository fileTypeTemplateRepository;
    
    @Autowired
    private AuditService auditService;

    @Override
    public List<FileTypeTemplate> getAllFileTypes() {
        return fileTypeTemplateRepository.findAllEnabled()
                .stream()
                .map(this::convertToFileTypeTemplate)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FileTypeTemplate> getFileTypeTemplate(String fileType) {
        Optional<FileTypeTemplateEntity> entity = fileTypeTemplateRepository.findByFileTypeAndEnabled(fileType, "Y");
        if (entity.isPresent()) {
            FileTypeTemplate template = convertToFileTypeTemplate(entity.get());
            // Get all transaction types for this file type
            List<String> transactionTypes = getTransactionTypesByFileType(fileType);
            List<FieldTemplate> allFields = new ArrayList<>();
            for (String txnType : transactionTypes) {
                allFields.addAll(getFieldTemplatesByFileTypeAndTransactionType(fileType, txnType));
            }
            template.setFields(allFields);
            return Optional.of(template);
        }
        return Optional.empty();
    }

    @Override
    public List<FieldTemplate> getFieldTemplatesByFileTypeAndTransactionType(String fileType, String transactionType) {
        return fieldTemplateRepository.findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(fileType, transactionType, "Y")
                .stream()
                .map(this::convertToFieldTemplate)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getTransactionTypesByFileType(String fileType) {
        return fieldTemplateRepository.findTransactionTypesByFileType(fileType);
    }

    @Override
    public FieldMappingConfig createConfigurationFromTemplate(String fileType, String transactionType, String sourceSystem, String jobName, String createdBy) {
        log.info("Creating configuration from template: {}/{} for {}.{}", fileType, transactionType, sourceSystem, jobName);
        
        List<FieldTemplate> templates = getFieldTemplatesByFileTypeAndTransactionType(fileType, transactionType);
        List<FieldMapping> mappings = new ArrayList<>();
        
        for (FieldTemplate template : templates) {
            FieldMapping mapping = new FieldMapping();
            mapping.setFieldName(template.getFieldName());
            mapping.setTargetField(template.getFieldName());
            mapping.setTargetPosition(template.getTargetPosition());  // Sequential 1,2,3,4...
            mapping.setLength(template.getLength());
            mapping.setDataType(template.getDataType().toLowerCase());
            mapping.setFormat(template.getFormat());
            mapping.setPad("right"); // Default padding
            mapping.setTransformationType("source"); // Default transformation
            // Note: sourceField to be filled by BA
            mappings.add(mapping);
        }
        
        return new FieldMappingConfig(
            sourceSystem, 
            jobName, 
            transactionType, 
            "Generated from " + fileType + "/" + transactionType + " template",
            mappings,
            LocalDateTime.now(),
            createdBy,
            1
        );
    }

    @Override
    public TemplateImportResult importFromExcel(MultipartFile file, String fileType, String createdBy) {
        // TODO: Implement Excel parsing using Apache POI
        log.info("Excel import requested for file type: {}", fileType);
        return new TemplateImportResult(false, fileType, 0, 0, 
            List.of("Excel import not yet implemented"), 
            List.of(), "Excel import feature coming soon");
    }

    @Override
    public ValidationResult validateTemplate(FileTypeTemplate template) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (template.getFileType() == null || template.getFileType().trim().isEmpty()) {
            errors.add("File type is required");
        }
        
        if (template.getFields() != null) {
            for (FieldTemplate field : template.getFields()) {
                ValidationResult fieldResult = validateFieldTemplate(field);
                errors.addAll(fieldResult.getErrors());
                warnings.addAll(fieldResult.getWarnings());
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validateFieldTemplate(FieldTemplate field) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (field.getFieldName() == null || field.getFieldName().trim().isEmpty()) {
            errors.add("Field name is required");
        }
        
        if (field.getTargetPosition() == null || field.getTargetPosition() < 1) {
            errors.add("Position must be greater than 0");
        }
        
        if (field.getLength() == null || field.getLength() < 1) {
            errors.add("Length must be greater than 0");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    // Helper methods
    private FileTypeTemplate convertToFileTypeTemplate(FileTypeTemplateEntity entity) {
        FileTypeTemplate template = new FileTypeTemplate();
        BeanUtils.copyProperties(entity, template);
        return template;
    }
    
    private FieldTemplate convertToFieldTemplate(FieldTemplateEntity entity) {
        FieldTemplate template = new FieldTemplate();
        BeanUtils.copyProperties(entity, template);
        return template;
    }

    // TODO: Implement remaining CRUD operations
    @Override
    public FileTypeTemplate createFileTypeTemplate(FileTypeTemplate template, String createdBy) {
        log.info("Creating file type template: {}", template.getFileType());
        FileTypeTemplateEntity entity = new FileTypeTemplateEntity();
        BeanUtils.copyProperties(template, entity);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(LocalDateTime.now());
        
        FileTypeTemplateEntity saved = fileTypeTemplateRepository.save(entity);
        auditService.logCreate(saved.getFileType(), template, createdBy, "File type template created");
        return convertToFileTypeTemplate(saved);
    }

    @Override
    public FileTypeTemplate updateFileTypeTemplate(FileTypeTemplate template, String modifiedBy) {
        log.info("Updating file type template: {}", template.getFileType());
        Optional<FileTypeTemplateEntity> existing = fileTypeTemplateRepository.findById(template.getFileType());
        if (existing.isPresent()) {
            FileTypeTemplateEntity entity = existing.get();
            FileTypeTemplate oldTemplate = convertToFileTypeTemplate(entity);
            
            BeanUtils.copyProperties(template, entity);
            entity.setModifiedBy(modifiedBy);
            entity.setModifiedDate(LocalDateTime.now());
            
            FileTypeTemplateEntity saved = fileTypeTemplateRepository.save(entity);
            auditService.logUpdate(saved.getFileType(), oldTemplate, template, modifiedBy, "File type template updated");
            return convertToFileTypeTemplate(saved);
        }
        throw new RuntimeException("File type template not found: " + template.getFileType());
    }

    @Override
    public void deleteFileTypeTemplate(String fileType, String deletedBy) {
        log.info("Deleting file type template: {}", fileType);
        Optional<FileTypeTemplateEntity> existing = fileTypeTemplateRepository.findById(fileType);
        if (existing.isPresent()) {
            FileTypeTemplate template = convertToFileTypeTemplate(existing.get());
            fileTypeTemplateRepository.deleteById(fileType);
            auditService.logDelete(fileType, template, deletedBy, "File type template deleted");
        }
    }

    @Override
    public Optional<FieldTemplate> getFieldTemplate(String fileType, String transactionType, String fieldName) {
        Optional<FieldTemplateEntity> entity = fieldTemplateRepository.findByFileTypeAndTransactionTypeAndFieldName(
            fileType, transactionType, fieldName);
        return entity.map(this::convertToFieldTemplate);
    }

    @Override
    public FieldTemplate createFieldTemplate(FieldTemplate template, String createdBy) {
        log.info("Creating field template: {}/{}/{}", template.getFileType(), template.getTransactionType(), template.getFieldName());
        FieldTemplateEntity entity = new FieldTemplateEntity();
        BeanUtils.copyProperties(template, entity);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(LocalDateTime.now());
        
        FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
        String auditKey = template.getFileType() + "/" + template.getTransactionType() + "/" + template.getFieldName();
        auditService.logCreate(auditKey, template, createdBy, "Field template created");
        return convertToFieldTemplate(saved);
    }

    @Override
    public FieldTemplate updateFieldTemplate(FieldTemplate template, String modifiedBy) {
        log.info("Updating field template: {}/{}/{}", template.getFileType(), template.getTransactionType(), template.getFieldName());
        Optional<FieldTemplateEntity> existing = fieldTemplateRepository.findByFileTypeAndTransactionTypeAndFieldName(
            template.getFileType(), template.getTransactionType(), template.getFieldName());
        
        if (existing.isPresent()) {
            FieldTemplateEntity entity = existing.get();
            FieldTemplate oldTemplate = convertToFieldTemplate(entity);
            
            BeanUtils.copyProperties(template, entity);
            entity.setModifiedBy(modifiedBy);
            entity.setModifiedDate(LocalDateTime.now());
            
            FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
            String auditKey = template.getFileType() + "/" + template.getTransactionType() + "/" + template.getFieldName();
            auditService.logUpdate(auditKey, oldTemplate, template, modifiedBy, "Field template updated");
            return convertToFieldTemplate(saved);
        }
        throw new RuntimeException("Field template not found");
    }

    @Override
    public void deleteFieldTemplate(String fileType, String transactionType, String fieldName, String deletedBy) {
        log.info("Deleting field template: {}/{}/{}", fileType, transactionType, fieldName);
        Optional<FieldTemplateEntity> existing = fieldTemplateRepository.findByFileTypeAndTransactionTypeAndFieldName(
            fileType, transactionType, fieldName);
        
        if (existing.isPresent()) {
            FieldTemplate template = convertToFieldTemplate(existing.get());
            fieldTemplateRepository.deleteById(new FieldTemplateId(fileType, transactionType, fieldName));
            String auditKey = fileType + "/" + transactionType + "/" + fieldName;
            auditService.logDelete(auditKey, template, deletedBy, "Field template deleted");
        }
    }

    @Override
    public TemplateImportResult importFromJson(TemplateImportRequest request) {
        log.info("Importing template from JSON: {}", request.getFileType());
        try {
            int imported = 0;
            for (FieldTemplate field : request.getFields()) {
                field.setFileType(request.getFileType());
                createFieldTemplate(field, request.getCreatedBy());
                imported++;
            }
            return new TemplateImportResult(true, request.getFileType(), imported, 0, 
                List.of(), List.of(), "Successfully imported " + imported + " fields");
        } catch (Exception e) {
            log.error("Error importing from JSON", e);
            return new TemplateImportResult(false, request.getFileType(), 0, 0, 
                List.of("Import failed: " + e.getMessage()), List.of(), "Import failed");
        }
    }
}
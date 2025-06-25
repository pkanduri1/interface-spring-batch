package com.truist.batch.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//Apache POI imports for Excel processing
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
//Spring transaction support
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.truist.batch.entity.FieldTemplateEntity;
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
		return fileTypeTemplateRepository.findAllEnabled().stream().map(this::convertToFileTypeTemplate)
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
		return fieldTemplateRepository
				.findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(fileType, transactionType, "Y")
				.stream().map(this::convertToFieldTemplate).collect(Collectors.toList());
	}

	@Override
	public List<String> getTransactionTypesByFileType(String fileType) {
		return fieldTemplateRepository.findTransactionTypesByFileType(fileType);
	}

	@Override
	public FieldMappingConfig createConfigurationFromTemplate(String fileType, String transactionType,
			String sourceSystem, String jobName, String createdBy) {
		log.info("Creating configuration from template: {}/{} for {}.{}", fileType, transactionType, sourceSystem,
				jobName);

		List<FieldTemplate> templates = getFieldTemplatesByFileTypeAndTransactionType(fileType, transactionType);
		List<FieldMapping> mappings = new ArrayList<>();

		for (FieldTemplate template : templates) {
			FieldMapping mapping = new FieldMapping();
			mapping.setFieldName(template.getFieldName());
			mapping.setTargetField(template.getFieldName());
			mapping.setTargetPosition(template.getTargetPosition()); // Sequential 1,2,3,4...
			mapping.setLength(template.getLength());
			mapping.setDataType(template.getDataType().toLowerCase());
			mapping.setFormat(template.getFormat());
			mapping.setPad("right"); // Default padding
			mapping.setTransformationType("constant"); // Default transformation
			// Note: sourceField to be filled by BA
			mappings.add(mapping);
		}

		return new FieldMappingConfig(sourceSystem, jobName, transactionType,
				"Generated from " + fileType + "/" + transactionType + " template", mappings, LocalDateTime.now(),
				createdBy, 1);
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

		return new ValidationResult(errors.isEmpty(), errors);
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
		//auditService.logCreate(saved.getFileType(), template, createdBy, "File type template created");
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
			auditService.logUpdate(saved.getFileType(), oldTemplate, template, modifiedBy,
					"File type template updated");
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
		Optional<FieldTemplateEntity> entity = fieldTemplateRepository
				.findByFileTypeAndTransactionTypeAndFieldName(fileType, transactionType, fieldName);
		return entity.map(this::convertToFieldTemplate);
	}

	@Override
	public FieldTemplate createFieldTemplate(FieldTemplate template, String createdBy) {
		log.info("Creating field template: {}/{}/{}", template.getFileType(), template.getTransactionType(),
				template.getFieldName());
		FieldTemplateEntity entity = new FieldTemplateEntity();
		BeanUtils.copyProperties(template, entity);
		entity.setCreatedBy(createdBy);
		entity.setCreatedDate(new Date());

		FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
		String auditKey = template.getFileType() + "/" + template.getTransactionType() + "/" + template.getFieldName();
		auditService.logCreate(auditKey, template, createdBy, "Field template created");
		return convertToFieldTemplate(saved);
	}

	@Override
	public FieldTemplate updateFieldTemplate(FieldTemplate template, String modifiedBy) {
		log.info("Updating field template: {}/{}/{}", template.getFileType(), template.getTransactionType(),
				template.getFieldName());
		Optional<FieldTemplateEntity> existing = fieldTemplateRepository.findByFileTypeAndTransactionTypeAndFieldName(
				template.getFileType(), template.getTransactionType(), template.getFieldName());

		if (existing.isPresent()) {
			FieldTemplateEntity entity = existing.get();
			FieldTemplate oldTemplate = convertToFieldTemplate(entity);

			BeanUtils.copyProperties(template, entity);
			entity.setModifiedBy(modifiedBy);
			entity.setModifiedDate(new Date());

			FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
			String auditKey = template.getFileType() + "/" + template.getTransactionType() + "/"
					+ template.getFieldName();
			auditService.logUpdate(auditKey, oldTemplate, template, modifiedBy, "Field template updated");
			return convertToFieldTemplate(saved);
		}
		throw new RuntimeException("Field template not found");
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
			return new TemplateImportResult(true, request.getFileType(), imported, 0, List.of(), List.of(),
					"Successfully imported " + imported + " fields");
		} catch (Exception e) {
			log.error("Error importing from JSON", e);
			return new TemplateImportResult(false, request.getFileType(), 0, 0,
					List.of("Import failed: " + e.getMessage()), List.of(), "Import failed");
		}
	}

	// Excel Parsing Helper Methods for TemplateServiceImpl.java
	// Add these methods to your TemplateServiceImpl class

	/**
	 * Helper method to safely extract string value from Excel cell
	 */
	private String getCellStringValue(Row row, int cellIndex) {
		Cell cell = row.getCell(cellIndex);
		if (cell == null)
			return null;

		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue().trim();
		case NUMERIC:
			// Handle numeric values that should be treated as strings
			return String.valueOf((long) cell.getNumericCellValue());
		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case FORMULA:
			try {
				return cell.getStringCellValue().trim();
			} catch (Exception e) {
				return String.valueOf(cell.getNumericCellValue());
			}
		default:
			return null;
		}
	}

	/**
	 * Helper method to safely extract integer value from Excel cell
	 */
	private Integer getCellIntegerValue(Row row, int cellIndex) {
		Cell cell = row.getCell(cellIndex);
		if (cell == null)
			return null;

		switch (cell.getCellType()) {
		case NUMERIC:
			return (int) cell.getNumericCellValue();
		case STRING:
			try {
				return Integer.parseInt(cell.getStringCellValue().trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(String.format("Invalid number format in cell %d: '%s'",
						cellIndex + 1, cell.getStringCellValue()));
			}
		case FORMULA:
			try {
				return (int) cell.getNumericCellValue();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Cannot evaluate formula in cell %d", cellIndex + 1));
			}
		default:
			return null;
		}
	}

	/**
	 * Parse a single Excel row into a FieldTemplate object
	 */
	private FieldTemplate parseFieldFromRow(Row row, String fileType, int rowNum) {
		try {
			// Extract values from each column with validation
			String fieldName = getCellStringValue(row, 0);
			Integer targetPosition = getCellIntegerValue(row, 1);
			Integer length = getCellIntegerValue(row, 2);
			String dataType = getCellStringValue(row, 3);
			String format = getCellStringValue(row, 4);
			String required = getCellStringValue(row, 5);
			String description = getCellStringValue(row, 6);
			String transactionType = getCellStringValue(row, 7);

			// Required field validation
			if (fieldName == null || fieldName.trim().isEmpty()) {
				throw new IllegalArgumentException("Field Name is required");
			}

			if (targetPosition == null || targetPosition <= 0) {
				throw new IllegalArgumentException("Target Position must be a positive number");
			}

			if (length == null || length <= 0) {
				throw new IllegalArgumentException("Length must be a positive number");
			}

			// Data type validation
			if (dataType == null || dataType.trim().isEmpty()) {
				dataType = "String"; // Default data type
			}

			// Validate data type values
			String normalizedDataType = dataType.trim();
			if (!isValidDataType(normalizedDataType)) {
				throw new IllegalArgumentException(String.format(
						"Invalid data type '%s'. Must be String, BigDecimal, Date, or Integer", normalizedDataType));
			}

			// Business rule validation
			validateFieldBusinessRules(fieldName.trim(), targetPosition, length, normalizedDataType);

			// Create and populate FieldTemplate
			FieldTemplate field = new FieldTemplate();
			field.setFileType(fileType);
			field.setTransactionType(
					transactionType != null && !transactionType.trim().isEmpty() ? transactionType.trim() : "default");
			field.setFieldName(fieldName.trim());
			field.setTargetPosition(targetPosition);
			field.setLength(length);
			field.setDataType(normalizedDataType);
			field.setFormat(format != null ? format.trim() : "");
			field.setRequired(required != null && "Y".equalsIgnoreCase(required.trim()) ? "Y" : "N");
			field.setDescription(description != null ? description.trim() : "");
			field.setEnabled("Y");

			return field;

		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Error parsing field data: %s", e.getMessage()), e);
		}
	}

	/**
	 * Validate data type is one of allowed values
	 */
	private boolean isValidDataType(String dataType) {
		return dataType.equalsIgnoreCase("String") || dataType.equalsIgnoreCase("BigDecimal")
				|| dataType.equalsIgnoreCase("Date") || dataType.equalsIgnoreCase("Integer")
				|| dataType.equalsIgnoreCase("Long")||dataType.equalsIgnoreCase("Numeric");
	}

	/**
	 * Validate business rules for individual fields
	 */
	private void validateFieldBusinessRules(String fieldName, Integer position, Integer length, String dataType) {
		List<String> errors = new ArrayList<>();

		// Field name validation
		if (!fieldName.matches("^[a-zA-Z][a-zA-Z0-9_-]*$")) {
			errors.add(String.format(
					"Field name '%s' must start with a letter and contain only letters, numbers, and underscores",
					fieldName));
		}

		if (fieldName.length() > 100) {
			errors.add(String.format("Field name '%s' exceeds maximum length of 100 characters", fieldName));
		}

		// Position validation
		if (position > 999) {
			errors.add(String.format("Target position %d exceeds maximum allowed value of 999", position));
		}

		// Length validation by data type
		switch (dataType.toLowerCase()) {
		case "date":
			if (length != 8 && length != 10) {
				errors.add(String.format("Date field '%s' should have length 8 (yyyyMMdd) or 10 (yyyy-MM-dd), found %d",
						fieldName, length));
			}
			break;
		case "bigdecimal":
			if (length > 20) {
				errors.add(String.format("BigDecimal field '%s' length should not exceed 20, found %d", fieldName,
						length));
			}
			if (length < 1) {
				errors.add(
						String.format("BigDecimal field '%s' length must be at least 1, found %d", fieldName, length));
			}
			break;
		case "string":
			if (length > 255) {
				errors.add(
						String.format("String field '%s' length should not exceed 255, found %d", fieldName, length));
			}
			break;
		case "integer":
		case "long":
			if (length > 19) {
				errors.add(
						String.format("Numeric field '%s' length should not exceed 19, found %d", fieldName, length));
			}
			break;
		}

		// Throw exception if any validation errors found
		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(String.join("; ", errors));
		}
	}

	/**
	 * Complete importFromExcel implementation - replace the TODO in
	 * TemplateServiceImpl
	 */
	@Override
	public TemplateImportResult importFromExcel(MultipartFile file, String fileType, String createdBy) {
		log.info("Starting Excel import for file type: {} by user: {}", fileType, createdBy);

		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		List<FieldTemplate> importedFields = new ArrayList<>();

		// File validation
		if (file.isEmpty()) {
			errors.add("Excel file is empty");
			return new TemplateImportResult(false, fileType, 0, 0, errors, warnings, "File validation failed");
		}
		// Add to importFromExcel() file validation section
		if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
			errors.add("Excel file is too large. Maximum size is 10MB");
			return new TemplateImportResult(false, fileType, 0, 0, errors, warnings, "File too large");
		}

		if (!isExcelFile(file)) {
			errors.add("File must be an Excel file (.xlsx or .xls)");
			return new TemplateImportResult(false, fileType, 0, 0, errors, warnings, "Invalid file format");
		}

		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);

			// Validate Excel structure
			ValidationResult structureValidation = validateExcelStructure(sheet);
			if (!structureValidation.isValid()) {
				return new TemplateImportResult(false, fileType, 0, 0, structureValidation.getErrors(), warnings,
						"Excel structure validation failed");
			}

			warnings.addAll(structureValidation.getWarnings());

			// Parse field data
			int totalRows = sheet.getLastRowNum();
			log.info("Processing {} data rows for file type: {}", totalRows, fileType);

			for (int rowNum = 1; rowNum <= totalRows; rowNum++) {
				Row row = sheet.getRow(rowNum);
				if (row == null || isEmptyRow(row)) {
					warnings.add(String.format("Row %d is empty and will be skipped", rowNum + 1));
					continue;
				}

				try {
					FieldTemplate field = parseFieldFromRow(row, fileType, rowNum);
					if (field != null) {
						importedFields.add(field);
					}

					// Progress logging for large files
					if (rowNum % 50 == 0) {
						log.info("Processed {}/{} rows", rowNum, totalRows);
					}

				} catch (Exception e) {
					errors.add(String.format("Row %d: %s", rowNum + 1, e.getMessage()));
				}
			}

			// Validate parsed fields
			validateFieldCollection(importedFields, errors, warnings);

			// Save to database if no critical errors
			int savedCount = 0;
			if (errors.isEmpty()) {
				savedCount = saveImportedFields(importedFields, fileType, createdBy);

				String successMessage = String.format(
						"Successfully imported %d fields for file type '%s'. %d fields saved to database.",
						importedFields.size(), fileType, savedCount);

				log.info(successMessage);
				return new TemplateImportResult(true, fileType, importedFields.size(), savedCount, errors, warnings,
						successMessage);
			} else {
				if (!errors.isEmpty()) {
				    log.error("First 5 validation errors:");
				    errors.stream().limit(5).forEach(log::error);
				}
				String errorMessage = String.format("Import failed for file type '%s'. Found %d validation errors.",
						fileType, errors.size());

				log.error(errorMessage);
				return new TemplateImportResult(false, fileType, importedFields.size(), 0, errors, warnings,
						errorMessage);
			}

		} catch (Exception e) {
			
			log.error("Excel processing failed for file type: " + fileType, e);
			errors.add("Failed to process Excel file: " + e.getMessage());
			return new TemplateImportResult(false, fileType, 0, 0, errors, warnings, "Excel processing failed");
		}
	}

	/**
	 * Validate Excel file structure and headers
	 */
	private ValidationResult validateExcelStructure(Sheet sheet) {
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		// Check if sheet has data
		if (sheet.getLastRowNum() < 1) {
			errors.add("Excel file must contain at least one data row (plus header row)");
			return new ValidationResult(false, errors);
		}

		// Validate header row
		Row headerRow = sheet.getRow(0);
		if (headerRow == null) {
			errors.add("Excel file must have a header row");
			return new ValidationResult();
		}

		// Expected column headers for template import
		String[] expectedHeaders = { "Field Name", "Target Position", "Length", "Data Type", "Format", "Required",
				"Description", "Transaction Type" };

		// Check each required column
		for (int i = 0; i < expectedHeaders.length; i++) {
			Cell cell = headerRow.getCell(i);
			String actualHeader = cell != null ? getCellStringValue(headerRow, i) : "";

			if (actualHeader == null || actualHeader.isEmpty()) {
				errors.add(String.format("Column %d: Missing header (expected '%s')", i + 1, expectedHeaders[i]));
			} else if (!expectedHeaders[i].equalsIgnoreCase(actualHeader)) {
				// Flexible header matching (warn but don't fail)
				warnings.add(String.format("Column %d: Expected '%s', found '%s' (will proceed)", i + 1,
						expectedHeaders[i], actualHeader));
			}
		}

		return new ValidationResult(errors.isEmpty(), errors, warnings, warnings, warnings, warnings, warnings);
	}

	/**
	 * Validate collection of parsed fields for business rules
	 */
	private void validateFieldCollection(List<FieldTemplate> fields, List<String> errors, List<String> warnings) {
		// Check for duplicate field names within same transaction type
		Map<String, Map<String, Integer>> fieldNameCounts = new HashMap<>();
		for (FieldTemplate field : fields) {
			fieldNameCounts.computeIfAbsent(field.getTransactionType(), k -> new HashMap<>())
					.merge(field.getFieldName(), 1, Integer::sum);
		}

		fieldNameCounts.forEach((txnType, nameMap) -> {
			nameMap.entrySet().stream().filter(entry -> entry.getValue() > 1)
					.forEach(entry -> errors
							.add(String.format("Duplicate field name '%s' found %d times in transaction type '%s'",
									entry.getKey(), entry.getValue(), txnType)));
		});

		// Check for duplicate positions within same transaction type
		Map<String, Map<Integer, String>> positionMaps = new HashMap<>();
		for (FieldTemplate field : fields) {
			Map<Integer, String> posMap = positionMaps.computeIfAbsent(field.getTransactionType(),
					k -> new HashMap<>());
			String existingField = posMap.put(field.getTargetPosition(), field.getFieldName());
			if (existingField != null) {
				errors.add(String.format("Position %d in transaction type '%s' is used by both '%s' and '%s'",
						field.getTargetPosition(), field.getTransactionType(), existingField, field.getFieldName()));
			}
		}

		// Check for position gaps within each transaction type (warn only)
		positionMaps.forEach((txnType, posMap) -> {
			List<Integer> positions = posMap.keySet().stream().sorted().collect(Collectors.toList());
			for (int i = 1; i < positions.size(); i++) {
				if (positions.get(i) - positions.get(i - 1) > 1) {
					warnings.add(String.format("Position gap in transaction type '%s': positions %d to %d are missing",
							txnType, positions.get(i - 1) + 1, positions.get(i) - 1));
				}
			}
		});
	}

	/**
	 * Save imported fields to database with transaction support
	 */
	@Transactional
	private int saveImportedFields(List<FieldTemplate> fields, String fileType, String createdBy) {
		try {
			// Optional: Clear existing fields for this file type and transaction type
			// Uncomment if you want to replace existing templates
			// Note: This would need to be done per transaction type
			// Set<String> transactionTypes = fields.stream()
			// .map(FieldTemplate::getTransactionType)
			// .collect(Collectors.toSet());
			//
			// for (String txnType : transactionTypes) {
			// fields.stream()
			// .filter(f -> f.getTransactionType().equals(txnType))
			// .forEach(field -> {
			// FieldTemplateId id = new FieldTemplateId(fileType, txnType,
			// field.getFieldName());
			// fieldTemplateRepository.deleteById(id);
			// });
			// }

			int savedCount = 0;
			LocalDateTime now = LocalDateTime.now();

			for (FieldTemplate field : fields) {
				// Convert FieldTemplate DTO to FieldTemplateEntity
				FieldTemplateEntity entity = convertToFieldTemplateEntity(field, createdBy, now);

				// Save entity using repository
				fieldTemplateRepository.save(entity);
				savedCount++;

				log.debug("Saved field template: {}/{}/{}", fileType, field.getTransactionType(), field.getFieldName());
			}

			log.info("Successfully saved {} field templates for file type: {}", savedCount, fileType);

			// Create audit entry
			/*
			 * createAuditEntry("TEMPLATE_IMPORT", fileType, createdBy,
			 * String.format("Imported %d fields from Excel", savedCount));
			 */

			return savedCount;

		} catch (Exception e) {
			log.error("Failed to save imported fields for file type: " + fileType, e);
			throw new RuntimeException("Database save failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Convert FieldTemplate DTO to FieldTemplateEntity for database persistence
	 */
	private FieldTemplateEntity convertToFieldTemplateEntity(FieldTemplate field, String createdBy, LocalDateTime now) {
		FieldTemplateEntity entity = new FieldTemplateEntity();

		// Set primary key fields (3-field composite key)
		entity.setFileType(field.getFileType());
		entity.setTransactionType(field.getTransactionType());
		entity.setFieldName(field.getFieldName());

		// Set other fields
		entity.setTargetPosition(field.getTargetPosition());
		entity.setLength(field.getLength());
		entity.setDataType(field.getDataType());
		entity.setFormat(field.getFormat());
		entity.setRequired(field.getRequired());
		entity.setDescription(field.getDescription());
		entity.setEnabled("Y");

		// Set audit fields
		entity.setCreatedBy(createdBy);
		entity.setCreatedDate(new Date());
		entity.setModifiedBy(createdBy);
		entity.setModifiedDate(new Date());
		entity.setVersion(1);

		return entity;
	}

	/**
	 * Create audit entry for template operations
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void createAuditEntry(String operation, String fileType, String user, String description) {
	    try {
	        if (auditService != null) {
	            String auditKey = operation + "_" + fileType;
	            // Don't let audit failures kill the import
	            auditService.logCreate(auditKey, fileType, user, description);
	        }
	    } catch (Exception e) {
	        log.warn("Audit failed (non-critical): {}", e.getMessage());
	        // Swallow the exception - don't fail the import
	    }
	}

	/**
	 * Utility methods
	 */
	private boolean isExcelFile(MultipartFile file) {
		String filename = file.getOriginalFilename();
		return filename != null
				&& (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"));
	}

	private boolean isEmptyRow(Row row) {
		for (int i = 0; i < 8; i++) { // Check first 8 columns
			Cell cell = row.getCell(i);
			if (cell != null && cell.getCellType() != CellType.BLANK) {
				String value = getCellStringValue(row, i);
				if (value != null && !value.trim().isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}
	
	// Add these methods to the TemplateServiceImpl class

	@Override
	@Transactional
	public FieldTemplate createFieldTemplate(FieldTemplate fieldTemplate) {
	    try {
	        // Validate the field template
	        ValidationResult validation = validateFieldTemplate(fieldTemplate);
	        if (!validation.isValid()) {
	            throw new IllegalArgumentException("Invalid field template: " + String.join(", ", validation.getErrors()));
	        }
	        
	        // Check for duplicates
	        Optional<FieldTemplateEntity> existing = fieldTemplateRepository
	            .findByFileTypeAndTransactionTypeAndFieldName(
	                fieldTemplate.getFileType(), 
	                fieldTemplate.getTransactionType(), 
	                fieldTemplate.getFieldName());
	        
	        if (existing.isPresent()) {
	            throw new IllegalArgumentException("Field template already exists: " + fieldTemplate.getFieldName());
	        }
	        
	        // Check for position conflicts
	        Optional<FieldTemplateEntity> positionConflict = fieldTemplateRepository
	            .findByFileTypeAndTransactionTypeAndTargetPosition(
	                fieldTemplate.getFileType(), 
	                fieldTemplate.getTransactionType(), 
	                fieldTemplate.getTargetPosition());
	        
	        if (positionConflict.isPresent()) {
	            throw new IllegalArgumentException("Position already occupied: " + fieldTemplate.getTargetPosition());
	        }
	        
	        // Create entity
	        FieldTemplateEntity entity = convertToFieldTemplateEntity(fieldTemplate);
	        entity.setCreatedDate(new Date());
	        entity.setEnabled("Y");
	        
	        // Save and audit
	        FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
	      //  auditService.logCreate("FIELD_TEMPLATE", saved.getFieldName(), fieldTemplate.getCreatedBy());
	        
	        log.info("Created field template: {} for {}/{}", 
	            fieldTemplate.getFieldName(), fieldTemplate.getFileType(), fieldTemplate.getTransactionType());
	        
	        return convertToFieldTemplate(saved);
	    } catch (Exception e) {
	        log.error("Error creating field template: {}", fieldTemplate.getFieldName(), e);
	        throw new RuntimeException("Failed to create field template", e);
	    }
	}

	@Override
	@Transactional
	public FieldTemplate updateFieldTemplate(FieldTemplate fieldTemplate) {
	    try {
	        // Find existing field
	        Optional<FieldTemplateEntity> existing = fieldTemplateRepository
	            .findByFileTypeAndTransactionTypeAndFieldName(
	                fieldTemplate.getFileType(), 
	                fieldTemplate.getTransactionType(), 
	                fieldTemplate.getFieldName());
	        
	        if (existing.isEmpty()) {
	            throw new IllegalArgumentException("Field template not found: " + fieldTemplate.getFieldName());
	        }
	        
	        FieldTemplateEntity entity = existing.get();
	        String oldValue = entity.toString();
	        
	        // Update fields
	        entity.setTargetPosition(fieldTemplate.getTargetPosition());
	        entity.setLength(fieldTemplate.getLength());
	        entity.setDataType(fieldTemplate.getDataType());
	        entity.setFormat(fieldTemplate.getFormat());
	        entity.setRequired(fieldTemplate.getRequired());
	        entity.setDescription(fieldTemplate.getDescription());
	        entity.setEnabled(fieldTemplate.getEnabled());
	        entity.setModifiedBy(fieldTemplate.getModifiedBy());
	        entity.setModifiedDate(new Date());
	        entity.setVersion(entity.getVersion() + 1);
	        
	        // Save and audit
	        FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
	        auditService.logUpdate("FIELD_TEMPLATE", saved.getFieldName(), 
	                              oldValue, saved.toString(), fieldTemplate.getModifiedBy());
	        
	        log.info("Updated field template: {} for {}/{}", 
	            fieldTemplate.getFieldName(), fieldTemplate.getFileType(), fieldTemplate.getTransactionType());
	        
	        return convertToFieldTemplate(saved);
	    } catch (Exception e) {
	        log.error("Error updating field template: {}", fieldTemplate.getFieldName(), e);
	        throw new RuntimeException("Failed to update field template", e);
	    }
	}

	@Override
	@Transactional
	public void deleteFieldTemplate(String fileType, String transactionType, String fieldName, String deletedBy) {
	    try {
	        Optional<FieldTemplateEntity> existing = fieldTemplateRepository
	            .findByFileTypeAndTransactionTypeAndFieldName(fileType, transactionType, fieldName);
	        
	        if (existing.isEmpty()) {
	            throw new IllegalArgumentException("Field template not found: " + fieldName);
	        }
	        
	        FieldTemplateEntity entity = existing.get();
	        String oldValue = entity.toString();
	        
	        // Soft delete - mark as disabled
	        entity.setEnabled("N");
	        entity.setModifiedBy(deletedBy);
	        entity.setModifiedDate(new Date());
	        entity.setVersion(entity.getVersion() + 1);
	        
	        fieldTemplateRepository.save(entity);
	        auditService.logDelete("FIELD_TEMPLATE", fieldName, oldValue, deletedBy);
	        
	        log.info("Deleted field template: {} for {}/{}", fieldName, fileType, transactionType);
	    } catch (Exception e) {
	        log.error("Error deleting field template: {}", fieldName, e);
	        throw new RuntimeException("Failed to delete field template", e);
	    }
	}

	@Override
	@Transactional
	public FieldTemplate duplicateFieldTemplate(String fileType, String transactionType, String fieldName, 
	                                           String newFieldName, Integer newPosition, String createdBy) {
	    try {
	        // Find original field
	        Optional<FieldTemplateEntity> original = fieldTemplateRepository
	            .findByFileTypeAndTransactionTypeAndFieldName(fileType, transactionType, fieldName);
	        
	        if (original.isEmpty()) {
	            throw new IllegalArgumentException("Original field template not found: " + fieldName);
	        }
	        
	        // Create duplicate
	        FieldTemplateEntity originalEntity = original.get();
	        FieldTemplate duplicateTemplate = convertToFieldTemplate(originalEntity);
	        duplicateTemplate.setFieldName(newFieldName);
	        duplicateTemplate.setTargetPosition(newPosition);
	        duplicateTemplate.setCreatedBy(createdBy);
	        
	        return createFieldTemplate(duplicateTemplate);
	    } catch (Exception e) {
	        log.error("Error duplicating field template: {} to {}", fieldName, newFieldName, e);
	        throw new RuntimeException("Failed to duplicate field template", e);
	    }
	}

	@Override
	@Transactional
	public List<FieldTemplate> bulkUpdateFieldTemplates(String fileType, List<FieldTemplate> fields) {
	    try {
	        List<FieldTemplate> results = new ArrayList<>();
	        
	        for (FieldTemplate field : fields) {
	            field.setFileType(fileType);
	            
	            // Check if field exists
	            Optional<FieldTemplateEntity> existing = fieldTemplateRepository
	                .findByFileTypeAndTransactionTypeAndFieldName(
	                    fileType, field.getTransactionType(), field.getFieldName());
	            
	            if (existing.isPresent()) {
	                // Update existing
	                results.add(updateFieldTemplate(field));
	            } else {
	                // Create new
	                results.add(createFieldTemplate(field));
	            }
	        }
	        
	        log.info("Bulk updated {} field templates for fileType: {}", fields.size(), fileType);
	        return results;
	    } catch (Exception e) {
	        log.error("Error bulk updating field templates for fileType: {}", fileType, e);
	        throw new RuntimeException("Failed to bulk update field templates", e);
	    }
	}

	@Override
	@Transactional
	public List<FieldTemplate> reorderFieldTemplates(String fileType, List<Map<String, Object>> fieldOrders, String modifiedBy) {
	    try {
	        List<FieldTemplate> results = new ArrayList<>();
	        
	        for (Map<String, Object> order : fieldOrders) {
	            String fieldName = (String) order.get("fieldName");
	            Integer newPosition = (Integer) order.get("newPosition");
	            String transactionType = (String) order.getOrDefault("transactionType", "default");
	            
	            Optional<FieldTemplateEntity> existing = fieldTemplateRepository
	                .findByFileTypeAndTransactionTypeAndFieldName(fileType, transactionType, fieldName);
	            
	            if (existing.isPresent()) {
	                FieldTemplateEntity entity = existing.get();
	                entity.setTargetPosition(newPosition);
	                entity.setModifiedBy(modifiedBy);
	                entity.setModifiedDate(new Date());
	                entity.setVersion(entity.getVersion() + 1);
	                
	                FieldTemplateEntity saved = fieldTemplateRepository.save(entity);
	                results.add(convertToFieldTemplate(saved));
	            }
	        }
	        
	        log.info("Reordered {} field templates for fileType: {}", fieldOrders.size(), fileType);
	        return results;
	    } catch (Exception e) {
	        log.error("Error reordering field templates for fileType: {}", fileType, e);
	        throw new RuntimeException("Failed to reorder field templates", e);
	    }
	}

	@Override
	public ValidationResult validateFieldTemplate(FieldTemplate fieldTemplate) {
	    ValidationResult result = new ValidationResult();
	    result.setValid(true);
	    
	    List<String> errors = new ArrayList<>();
	    List<String> warnings = new ArrayList<>();
	    
	    // Required field validation
	    if (fieldTemplate.getFieldName() == null || fieldTemplate.getFieldName().trim().isEmpty()) {
	        errors.add("Field name is required");
	    }
	    
	    if (fieldTemplate.getFileType() == null || fieldTemplate.getFileType().trim().isEmpty()) {
	        errors.add("File type is required");
	    }
	    
	    if (fieldTemplate.getTransactionType() == null || fieldTemplate.getTransactionType().trim().isEmpty()) {
	        errors.add("Transaction type is required");
	    }
	    
	    if (fieldTemplate.getTargetPosition() == null || fieldTemplate.getTargetPosition() <= 0) {
	        errors.add("Target position must be greater than 0");
	    }
	    
	    if (fieldTemplate.getLength() == null || fieldTemplate.getLength() <= 0) {
	        errors.add("Length must be greater than 0");
	    }
	    
	    if (fieldTemplate.getDataType() == null || fieldTemplate.getDataType().trim().isEmpty()) {
	        errors.add("Data type is required");
	    }
	    
	    // Business logic validation
	    if (fieldTemplate.getFieldName() != null && fieldTemplate.getFieldName().length() > 50) {
	        errors.add("Field name cannot exceed 50 characters");
	    }
	    
	    if (fieldTemplate.getTargetPosition() != null && fieldTemplate.getTargetPosition() > 10000) {
	        warnings.add("Position seems unusually high: " + fieldTemplate.getTargetPosition());
	    }
	    
	    if (fieldTemplate.getLength() != null && fieldTemplate.getLength() > 1000) {
	        warnings.add("Field length seems unusually large: " + fieldTemplate.getLength());
	    }
	    
	    result.setErrors(errors);
	    result.setWarnings(warnings);
	    result.setValid(errors.isEmpty());
	    
	    return result;
	}

	@Override
	public ValidationResult validateFieldTemplates(String fileType, List<FieldTemplate> fields) {
	    ValidationResult result = new ValidationResult();
	    result.setValid(true);
	    
	    List<String> errors = new ArrayList<>();
	    List<String> warnings = new ArrayList<>();
	    
	    // Individual field validation
	    for (FieldTemplate field : fields) {
	        ValidationResult fieldResult = validateFieldTemplate(field);
	        if (!fieldResult.isValid()) {
	            errors.addAll(fieldResult.getErrors().stream()
	                .map(error -> field.getFieldName() + ": " + error)
	                .toList());
	        }
	        warnings.addAll(fieldResult.getWarnings().stream()
	            .map(warning -> field.getFieldName() + ": " + warning)
	            .toList());
	    }
	    
	    // Cross-field validation
	    Set<String> fieldNames = new HashSet<>();
	    Set<Integer> positions = new HashSet<>();
	    
	    for (FieldTemplate field : fields) {
	        // Check for duplicate field names
	        if (!fieldNames.add(field.getFieldName())) {
	            errors.add("Duplicate field name: " + field.getFieldName());
	        }
	        
	        // Check for duplicate positions within same transaction type
	        String positionKey = field.getTransactionType() + "-" + field.getTargetPosition();
	        if (!positions.add(field.getTargetPosition())) {
	            errors.add("Duplicate position " + field.getTargetPosition() + 
	                      " in transaction type " + field.getTransactionType());
	        }
	    }
	    
	    result.setErrors(errors);
	    result.setWarnings(warnings);
	    result.setValid(errors.isEmpty());
	    
	    return result;
	}

	/**
	 * Helper method to convert FieldTemplate to FieldTemplateEntity
	 */
	private FieldTemplateEntity convertToFieldTemplateEntity(FieldTemplate fieldTemplate) {
	    FieldTemplateEntity entity = new FieldTemplateEntity();
	    entity.setFileType(fieldTemplate.getFileType());
	    entity.setTransactionType(fieldTemplate.getTransactionType());
	    entity.setFieldName(fieldTemplate.getFieldName());
	    entity.setTargetPosition(fieldTemplate.getTargetPosition());
	    entity.setLength(fieldTemplate.getLength());
	    entity.setDataType(fieldTemplate.getDataType());
	    entity.setFormat(fieldTemplate.getFormat());
	    entity.setRequired(fieldTemplate.getRequired());
	    entity.setDescription(fieldTemplate.getDescription());
	    entity.setEnabled(fieldTemplate.getEnabled() != null ? fieldTemplate.getEnabled() : "Y");
	    entity.setCreatedBy(fieldTemplate.getCreatedBy());
	    entity.setModifiedBy(fieldTemplate.getModifiedBy());
	    entity.setVersion(1);
	    return entity;
	}

}
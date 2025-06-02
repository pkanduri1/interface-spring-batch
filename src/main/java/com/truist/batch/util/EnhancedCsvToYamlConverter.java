package com.truist.batch.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced CSV to YAML converter that separates target definitions from source mappings.
 */
@Slf4j
public class EnhancedCsvToYamlConverter {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java EnhancedCsvToYamlConverter <targetName> <sourceSystem> <csvFile>");
            return;
        }
        
        convertCsvToSeparateYamls(args[0], args[1], args[2]);
    }
    
    public static void convertCsvToSeparateYamls(String targetName, String sourceSystem, String csvFilePath) 
            throws Exception {
        
        log.info("🚀 Converting: {} + {} from {}", targetName, sourceSystem, csvFilePath);
        
        List<CsvFieldRow> csvRows = parseCsvFile(csvFilePath);
        log.info("📊 Parsed {} field definitions", csvRows.size());
        
        extractTargetDefinition(targetName, csvRows); // Creates multiple target files
        SourceTargetMapping sourceMapping = extractSourceMapping(targetName, sourceSystem, csvRows);
        
        writeSourceMapping(sourceMapping);
        
        log.info("✅ Conversion complete!");
        log.info("📁 Target: src/main/resources/targets/{}-[txnType].yml", targetName);
        log.info("📁 Mapping: src/main/resources/mappings/{}/{}-mapping.yml", sourceSystem, targetName);
    }
    
    private static List<CsvFieldRow> parseCsvFile(String csvFilePath) throws IOException {
        List<CsvFieldRow> rows = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(csvFilePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            String[] headers = headerLine.split(",");
            Map<String, Integer> headerMap = createHeaderMap(headers);
            
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    CsvFieldRow row = parseCsvRow(line, headerMap, lineNumber);
                    if (row != null) {
                        rows.add(row);
                    }
                } catch (Exception e) {
                    log.error("❌ Error parsing line {}: {}", lineNumber, line, e);
                    throw e;
                }
            }
        }
        
        rows.sort(Comparator.comparing(CsvFieldRow::getTargetPosition));
        return rows;
    }
    
    private static Map<String, Integer> createHeaderMap(String[] headers) {
        Map<String, Integer> headerMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = normalizeHeader(headers[i]);
            headerMap.put(normalizedHeader, i);
        }
        
        String[] requiredHeaders = {
            "fieldname", "targetposition", "length", "datatype", "targetfieldname"
        };
        
        for (String required : requiredHeaders) {
            if (!headerMap.containsKey(required)) {
                throw new IllegalArgumentException("Missing required header: " + required);
            }
        }
        
        return headerMap;
    }
    
    private static String normalizeHeader(String header) {
        return header.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }
    
    private static CsvFieldRow parseCsvRow(String line, Map<String, Integer> headerMap, int lineNumber) {
        String[] tokens = line.split(",", -1);
        
        CsvFieldRow row = new CsvFieldRow();
        
        try {
            row.setFieldName(getToken(tokens, headerMap, "fieldname"));
            row.setTargetPosition(Integer.parseInt(getToken(tokens, headerMap, "targetposition")));
            row.setLength(Integer.parseInt(getToken(tokens, headerMap, "length")));
            row.setDataType(getToken(tokens, headerMap, "datatype"));
            row.setTargetFieldName(getToken(tokens, headerMap, "targetfieldname"));
            
            row.setFormat(getTokenOrDefault(tokens, headerMap, "format", ""));
            row.setPad(getTokenOrDefault(tokens, headerMap, "pad", "right"));
            row.setTransformationType(getTokenOrDefault(tokens, headerMap, "transformationtype", "constant"));
            row.setTransform(getTokenOrDefault(tokens, headerMap, "transform", ""));
            row.setDefaultValue(getTokenOrDefault(tokens, headerMap, "defaultvalue", ""));
            row.setSourceFieldName(getTokenOrDefault(tokens, headerMap, "sourcefieldname", ""));
            row.setIfExpr(getTokenOrDefault(tokens, headerMap, "if", ""));
            row.setElseIfExpr(getTokenOrDefault(tokens, headerMap, "elseif", ""));
            row.setElseExpr(getTokenOrDefault(tokens, headerMap, "else", ""));
            row.setTransactionType(getTokenOrDefault(tokens, headerMap, "transactiontype", "default"));
            
            return row;
            
        } catch (Exception e) {
            throw new RuntimeException("Error parsing line " + lineNumber + ": " + line, e);
        }
    }
    
    private static String getToken(String[] tokens, Map<String, Integer> headerMap, String header) {
        Integer index = headerMap.get(header);
        if (index == null || index >= tokens.length) {
            throw new IllegalArgumentException("Missing required field: " + header);
        }
        return tokens[index].trim();
    }
    
    private static String getTokenOrDefault(String[] tokens, Map<String, Integer> headerMap, String header, String defaultValue) {
        Integer index = headerMap.get(header);
        if (index == null || index >= tokens.length) {
            return defaultValue;
        }
        String value = tokens[index].trim();
     // Remove surrounding quotes
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.isEmpty() ? defaultValue : value;
    }
    
    private static TargetDefinition extractTargetDefinition(String targetName, List<CsvFieldRow> csvRows) {
        // Group by transaction type
        Map<String, List<CsvFieldRow>> byTxnType = csvRows.stream()
            .collect(Collectors.groupingBy(row -> 
                row.getTransactionType() != null ? row.getTransactionType() : "default"));
        
        // Create separate target definition for each transaction type
        for (Map.Entry<String, List<CsvFieldRow>> entry : byTxnType.entrySet()) {
            String txnType = entry.getKey();
            List<CsvFieldRow> rows = entry.getValue();
            
            TargetDefinition target = new TargetDefinition();
            target.setTargetName(targetName + "-" + txnType); // e.g. "atoctran-200"
            target.setDescription("Generated from CSV for transaction type " + txnType);
            target.setVersion("1.0");
            target.setFileType(FileType.FIXED_WIDTH);
            
            List<TargetField> targetFields = rows.stream()
                .map(EnhancedCsvToYamlConverter::convertToTargetField)
                .collect(Collectors.toList());
            
            target.setFields(targetFields);
            
            int totalLength = targetFields.stream()
                .mapToInt(TargetField::getLength)
                .sum();
            target.setRecordLength(totalLength);
            
            // Write separate target file
            try {
                writeTargetDefinition(target);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write target definition", e);
            }
        }
        
        // Return first one for compatibility (not used in new approach)
        return new TargetDefinition();
    }
    
    private static TargetField convertToTargetField(CsvFieldRow row) {
        TargetField field = new TargetField();
        field.setName(row.getTargetFieldName());
        field.setPosition(row.getTargetPosition());
        field.setLength(row.getLength());
        field.setDataType(parseDataType(row.getDataType()));
        field.setFormat(row.getFormat());
        field.setDefaultValue(row.getDefaultValue());
        field.setDescription("Generated from CSV field: " + row.getFieldName());
        
        if (row.getPad() != null && !row.getPad().isEmpty()) {
            PaddingConfig padding = new PaddingConfig();
            padding.setSide(parsePaddingSide(row.getPad()));
            
            String padChar = determinePaddingCharacter(row.getDataType(), row.getPad());
            padding.setCharacter(padChar);
            
            field.setPadding(padding);
        }
        
        return field;
    }
    
    private static SourceTargetMapping extractSourceMapping(String targetName, String sourceSystem, List<CsvFieldRow> csvRows) {
        SourceTargetMapping mapping = new SourceTargetMapping();
        mapping.setSourceSystem(sourceSystem);
        mapping.setTargetName(targetName);
        mapping.setDescription(sourceSystem + " system to " + targetName + " target mapping");
        
        Map<String, EnhancedFieldMapping> defaults = new HashMap<>();
        Map<String, Map<String, EnhancedFieldMapping>> mappings = new HashMap<>();
        Map<String, Map<String, EnhancedFieldMapping>> transactionMappings = new HashMap<>();
        
        for (CsvFieldRow row : csvRows) {
            EnhancedFieldMapping fieldMapping = convertToSourceMapping(row);
            String fieldName = row.getTargetFieldName();
            String transactionType = row.getTransactionType();
            
            if (isDefaultField(row)) {
                defaults.put(fieldName, fieldMapping);
            } else if ("default".equals(transactionType)) {
                mappings.computeIfAbsent("default", k -> new HashMap<>()).put(fieldName, fieldMapping);
            } else {
                transactionMappings.computeIfAbsent(transactionType, k -> new HashMap<>()).put(fieldName, fieldMapping);
            }
        }
        
        mapping.setDefaults(defaults);
        mapping.setMappings(mappings);
        mapping.setTransactionMappings(transactionMappings);
        
        log.info("🔗 Source mapping: {} defaults, {} general, {} transaction-specific", 
                defaults.size(), 
                mappings.values().stream().mapToInt(Map::size).sum(),
                transactionMappings.values().stream().mapToInt(Map::size).sum());
        
        return mapping;
    }
    
    private static EnhancedFieldMapping convertToSourceMapping(CsvFieldRow row) {
        EnhancedFieldMapping mapping = new EnhancedFieldMapping();
        
        MappingType type = parseMappingType(row.getTransformationType());
        mapping.setType(type);
        
        switch (type) {
            case CONSTANT:
                mapping.setValue(row.getDefaultValue());
                break;
                
            case SOURCE_FIELD:
                mapping.setSourceField(row.getSourceFieldName());
                mapping.setFallback(row.getDefaultValue());
                break;
                
            case CONDITIONAL:
                mapping.setSourceField(row.getSourceFieldName());
                mapping.setFallback(row.getDefaultValue());
                break;
                
            case COMPOSITE:
                mapping.setTransform(row.getTransform());
                mapping.setFallback(row.getDefaultValue());
                break;
                
            case BLANK:
                mapping.setValue(" ");
                break;
                
            default:
                mapping.setSourceField(row.getSourceFieldName());
                mapping.setFallback(row.getDefaultValue());
        }
        
        return mapping;
    }
    
    private static void writeTargetDefinition(TargetDefinition target) throws IOException {
        Path targetDir = Paths.get("src/main/resources/targets");
        Files.createDirectories(targetDir);
        
        Path targetFile = targetDir.resolve(target.getTargetName() + ".yml");
        yamlMapper.writeValue(targetFile.toFile(), target);
        
        log.info("📁 Wrote target definition: {}", targetFile);
    }
    
    private static void writeSourceMapping(SourceTargetMapping mapping) throws IOException {
        Path mappingDir = Paths.get("src/main/resources/mappings/" + mapping.getSourceSystem());
        Files.createDirectories(mappingDir);
        
        Path mappingFile = mappingDir.resolve(mapping.getTargetName() + "-mapping.yml");
        yamlMapper.writeValue(mappingFile.toFile(), mapping);
        
        log.info("📁 Wrote source mapping: {}", mappingFile);
    }
    
    // Helper methods
    private static void validateFieldConsistency(CsvFieldRow existing, CsvFieldRow current) {
        if (!existing.getLength().equals(current.getLength()) ||
            !existing.getTargetPosition().equals(current.getTargetPosition()) ||
            !existing.getDataType().equalsIgnoreCase(current.getDataType())) {
            
            log.warn("⚠️ Field inconsistency for {}: existing={}:{}, current={}:{}", 
                    existing.getTargetFieldName(),
                    existing.getLength(), existing.getTargetPosition(),
                    current.getLength(), current.getTargetPosition());
        }
    }
    
    private static boolean isDefaultField(CsvFieldRow row) {
        return "constant".equalsIgnoreCase(row.getTransformationType()) ||
               "blank".equalsIgnoreCase(row.getTransformationType()) ||
               (row.getSourceFieldName() == null || row.getSourceFieldName().trim().isEmpty());
    }
    
    private static DataType parseDataType(String dataType) {
        if (dataType == null) return DataType.STRING;
        
        switch (dataType.toLowerCase()) {
            case "numeric": case "number": case "decimal": return DataType.NUMERIC;
            case "date": case "datetime": return DataType.DATE;
            case "boolean": case "bool": return DataType.BOOLEAN;
            default: return DataType.STRING;
        }
    }
    
    private static PaddingSide parsePaddingSide(String pad) {
        if (pad == null) return PaddingSide.RIGHT;
        return "left".equalsIgnoreCase(pad) ? PaddingSide.LEFT : PaddingSide.RIGHT;
    }
    
    private static String determinePaddingCharacter(String dataType, String padSide) {
        if ("numeric".equalsIgnoreCase(dataType) && "left".equalsIgnoreCase(padSide)) {
            return "0";
        }
        return " ";
    }
    
    private static MappingType parseMappingType(String transformationType) {
        if (transformationType == null) return MappingType.CONSTANT;
        
        switch (transformationType.toLowerCase()) {
            case "source": return MappingType.SOURCE_FIELD;
            case "constant": return MappingType.CONSTANT;
            case "conditional": return MappingType.CONDITIONAL;
            case "composite": return MappingType.COMPOSITE;
            case "blank": return MappingType.BLANK;
            default: return MappingType.CONSTANT;
        }
    }
    
    private static void validateGenerated(TargetDefinition target, SourceTargetMapping mapping) {
        log.info("🔍 Validating generated files...");
        
        List<Integer> positions = target.getFields().stream()
            .map(TargetField::getPosition)
            .sorted()
            .collect(Collectors.toList());
            
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i) != i + 1) {
                throw new IllegalStateException("Field positions must be sequential starting from 1");
            }
        }
        
        log.info("✅ Validation passed");
    }
    
    // CSV row data class
    private static class CsvFieldRow {
        private String fieldName;
        private Integer targetPosition;
        private Integer length;
        private String dataType;
        private String format;
        private String pad;
        private String transformationType;
        private String transform;
        private String defaultValue;
        private String targetFieldName;
        private String sourceFieldName;
        private String ifExpr;
        private String elseIfExpr;
        private String elseExpr;
        private String transactionType;
        
        // Standard getters and setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public Integer getTargetPosition() { return targetPosition; }
        public void setTargetPosition(Integer targetPosition) { this.targetPosition = targetPosition; }
        
        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public String getPad() { return pad; }
        public void setPad(String pad) { this.pad = pad; }
        
        public String getTransformationType() { return transformationType; }
        public void setTransformationType(String transformationType) { this.transformationType = transformationType; }
        
        public String getTransform() { return transform; }
        public void setTransform(String transform) { this.transform = transform; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        
        public String getTargetFieldName() { return targetFieldName; }
        public void setTargetFieldName(String targetFieldName) { this.targetFieldName = targetFieldName; }
        
        public String getSourceFieldName() { return sourceFieldName; }
        public void setSourceFieldName(String sourceFieldName) { this.sourceFieldName = sourceFieldName; }
        
        public String getIfExpr() { return ifExpr; }
        public void setIfExpr(String ifExpr) { this.ifExpr = ifExpr; }
        
        public String getElseIfExpr() { return elseIfExpr; }
        public void setElseIfExpr(String elseIfExpr) { this.elseIfExpr = elseIfExpr; }
        
        public String getElseExpr() { return elseExpr; }
        public void setElseExpr(String elseExpr) { this.elseExpr = elseExpr; }
        
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    }
}
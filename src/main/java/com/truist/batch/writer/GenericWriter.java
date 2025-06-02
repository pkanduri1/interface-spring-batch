package com.truist.batch.writer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.YamlMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class GenericWriter implements ItemWriter<Map<String, Object>>, ItemStream {

    private FixedWidthFileWriter delegate;
    private final YamlMappingService yamlMappingService;
    private final FileConfig fileConfig;
    private List<Map.Entry<String, FieldMapping>> mappings;

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
        List<String> lines = chunk.getItems().stream().map(record -> {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, FieldMapping> entry : mappings) {
            	String value = (String) record.get(entry.getValue().getTargetField());
                sb.append(value != null ? value : "");            }
            return sb.toString();
        }).collect(Collectors.toList());
        
        Chunk<String> strChunk = new Chunk<>(lines);
        delegate.write(strChunk);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            // Get resolved output path with date formatting and placeholder resolution
            String outputPath = getResolvedOutputPath();
            log.info("📁 Creating output file: {}", outputPath);
            
            // Ensure parent directories exist
            File outFile = new File(outputPath);
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean created = parent.mkdirs();
                if (created) {
                    log.info("📂 Created directories: {}", parent.getAbsolutePath());
                } else {
                    log.warn("⚠️ Failed to create directories: {}", parent.getAbsolutePath());
                }
            }
            
            // ✅ FIX: Use transaction-type-aware loading instead of single-document loading
            String transactionType = fileConfig.getTransactionType();
            if (transactionType == null || transactionType.isEmpty()) {
                transactionType = "default";
            }
            
            log.debug("🎯 Loading mapping for template: {}, transactionType: {}", 
                fileConfig.getTemplate(), transactionType);
            
            // Get the specific mapping document for this transaction type
            YamlMapping yamlMapping = yamlMappingService.getMapping(fileConfig.getTemplate(), transactionType);
            
            // Convert to sorted list of entries
            mappings = yamlMapping.getFields().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                    e1.getValue().getTargetPosition(), 
                    e2.getValue().getTargetPosition()))
                .collect(Collectors.toList());
            
            log.debug("📋 Loaded {} field mappings for template: {}, transactionType: {}", 
                mappings.size(), fileConfig.getTemplate(), transactionType);
            
            // Initialize delegate writer
            delegate = new FixedWidthFileWriter(yamlMappingService, fileConfig.getTemplate(), outputPath);
            delegate.open(executionContext);
            
        } catch (Exception e) {
            log.error("❌ Failed to open GenericWriter: {}", e.getMessage(), e);
            throw new ItemStreamException("Failed to initialize writer", e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (delegate != null) {
            delegate.update(executionContext);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (delegate != null) {
            delegate.close();
        }
    }
    
    /**
     * Gets the output path with proper date formatting and directory creation.
     * Handles SpEL expressions and creates directories if they don't exist.
     */
    private String getResolvedOutputPath() {
        String outputPath = fileConfig.getParams().get("outputPath");
        if (outputPath == null) {
            throw new IllegalStateException("No outputPath configured");
        }
        
        // If path contains date placeholders, resolve them
        if (outputPath.contains("#{") || outputPath.contains("${DATE}") || outputPath.contains("${TIMESTAMP}")) {
            outputPath = resolveDatePlaceholders(outputPath);
        }
        
        return outputPath;
    }
    
    /**
     * Simple date placeholder resolution for output paths
     */
    private String resolveDatePlaceholders(String path) {
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // Replace common placeholders
        String result = path;
        result = result.replace("${DATE}", today);
        result = result.replace("${TIMESTAMP}", timestamp);
        
        // Handle complex SpEL expressions by replacing with simple date
        if (result.contains("#{T(java.time.LocalDate)")) {
            // Replace the entire complex SpEL expression with simple date
            result = result.replaceAll("#\\{T\\(java\\.time\\.LocalDate\\).*?\\}", today);
        }
        
        return result;
    }
}
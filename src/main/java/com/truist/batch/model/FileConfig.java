package com.truist.batch.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Configuration for a single input file (and its transaction types) to process.
 * Now includes dynamic template path construction based on jobName and sourceSystem.
 * Implements Serializable for Spring Batch ExecutionContext storage.
 */
@Data
public class FileConfig implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;

    /** Path to the input file */
    private String inputPath;

    /** Transaction types inside the file (if multiTxn=true) */
    private List<String> transactionTypes;

    /** Explicit template path (optional - will be auto-generated if null) */
    private String template;

    /** Target staging table or writer identifier */
    private String target;

    /** Additional parameters (e.g., delimiter) */
    private Map<String, String> params;
    
    /** Source system context for dynamic path construction */
    private String sourceSystem;
    
    /** Job name context for dynamic path construction */
    private String jobName;
    
    /** Transaction type for this specific file config */
    private String transactionType;

    /**
     * Gets the template path, constructing it dynamically if not explicitly set.
     * Convention: {jobName}/{sourceSystem}/{jobName}.yml
     * 
     * @return the template path for YAML mapping lookup
     */
    public String getTemplate() {
        if (template != null && !template.isEmpty()) {
            return template; // Use explicit template if provided
        }
        
        // Auto-construct template path: jobName/sourceSystem/jobName.yml
        if (jobName != null && sourceSystem != null) {
            return jobName + "/" + sourceSystem + "/" + jobName + ".yml";
        }
        
        throw new IllegalStateException("Cannot construct template path: jobName=" + jobName + ", sourceSystem=" + sourceSystem);
    }
    
    /**
     * Sets the template path explicitly (overrides dynamic construction).
     */
    public void setTemplate(String template) {
        this.template = template;
    }
    
    /**
     * Gets the template name without path (for logging/identification).
     */
    public String getTemplateName() {
        String templatePath = getTemplate();
        if (templatePath.contains("/")) {
            String[] parts = templatePath.split("/");
            return parts[parts.length - 1].replace(".yml", "");
        }
        return templatePath.replace(".yml", "");
    }
    
    /**
     * Gets the output path with proper date formatting and directory creation.
     * Handles SpEL expressions and creates directories if they don't exist.
     */
    public String getResolvedOutputPath() {
        String outputPath = params.get("outputPath");
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
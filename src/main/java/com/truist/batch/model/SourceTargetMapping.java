package com.truist.batch.model;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

@Data
public class SourceTargetMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Source system identifier (hr, shaw, etc.)
     */
    private String sourceSystem;
    
    /**
     * Target name (p327, atoctran, etc.)
     */
    private String targetName;
    
    /**
     * Description of this mapping
     */
    private String description;
    
    /**
     * Default field mappings that apply to all transaction types
     * Map: fieldName -> EnhancedFieldMapping
     */
    private Map<String, EnhancedFieldMapping> defaults;
    
    /**
     * General mappings by transaction type
     * Map: transactionType -> (fieldName -> EnhancedFieldMapping)
     */
    private Map<String, Map<String, EnhancedFieldMapping>> mappings;
    
    /**
     * Transaction-specific mappings (overrides defaults)
     * Map: transactionType -> (fieldName -> EnhancedFieldMapping)
     */
    private Map<String, Map<String, EnhancedFieldMapping>> transactionMappings;
}
package com.truist.batch.model;

import lombok.Data;

@Data
public class EnhancedFieldMapping {
    private MappingType type;
    private String sourceField;
    private String value;
    private String transform;
    private String fallback;
    // Add these to your existing FieldMapping class or create this as a new class
private String operation;
    
    /**
     * For COMPOSITE type - list of source fields
     * Example: ["PRINCIPAL_BAL", "INTEREST_BAL"]
     */
    private java.util.List<String> sourceFields;
    
    /**
     * For COMPOSITE type - delimiter for concatenation
     * Example: ",", "|", ""
     */
    private String delimiter;
    
    /**
     * For CONDITIONAL type - list of conditions
     */
    private java.util.List<ConditionalRule> conditions;
}	

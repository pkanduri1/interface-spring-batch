package com.truist.batch.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    
	private String fieldName;
    private String value;
    private String sourceField;
    private String targetField;
    private String from;
    private int length;
    private String pad;
    private String padChar;
    private boolean composite;
    private List<Map<String, String>> sources;
    private String transform;
    private String delimiter;
    private String format;
    private String sourceFormat;
    private String targetFormat;
    private String transformationType;
    private List<Condition> conditions;
    private int targetPosition;
    private String dataType;
    private String defaultValue;
    
    
 // ✅ ADD: Helper method for future DSL support
    private String expression; // For DSL expressions (future enhancement)
    
    public boolean hasDSLExpression() {
        return expression != null && !expression.trim().isEmpty();
    }
}
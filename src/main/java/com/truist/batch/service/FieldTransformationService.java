package com.truist.batch.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.truist.batch.model.EnhancedFieldMapping;
import com.truist.batch.model.MappingType;

import lombok.extern.slf4j.Slf4j;

/**
 * Service handling field transformation logic separated from processor
 */
@Service
@Slf4j
public class FieldTransformationService {

    /**
     * Transform field value based on mapping configuration
     */
    public String transformField(Map<String, Object> row, EnhancedFieldMapping mapping, String fallbackValue) {
        if (mapping == null) {
            return fallbackValue != null ? fallbackValue : " ";
        }
        
        String value = null;
        
        switch (mapping.getType()) {
            case CONSTANT:
                value = mapping.getValue();
                break;
                
            case SOURCE_FIELD:
                value = getSourceValue(row, mapping.getSourceField());
                if (value == null) {
                    value = mapping.getFallback();
                }
                break;
                
            case COMPOSITE:
                value = handleComposite(row, mapping);
                break;
                
            case CONDITIONAL:
                value = handleConditional(row, mapping);
                break;
                
            case BLANK:
                value = " ";
                break;
                
            default:
                value = mapping.getFallback();
        }
        
        return value != null ? value : fallbackValue;
    }
    
    /**
     * Get value from source row (case-insensitive)
     */
    private String getSourceValue(Map<String, Object> row, String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        
        // Try exact match first
        Object value = row.get(fieldName);
        if (value != null) {
            return value.toString();
        }
        
        // Case-insensitive search
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (fieldName.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() != null ? entry.getValue().toString() : null;
            }
        }
        
        return null;
    }
    
    /**
     * Handle composite transformations (sum, concat)
     */
    private String handleComposite(Map<String, Object> row, EnhancedFieldMapping mapping) {
        if (mapping.getSourceFields() == null || mapping.getSourceFields().isEmpty()) {
            return mapping.getFallback();
        }
        
        if ("sum".equalsIgnoreCase(mapping.getOperation())) {
            double sum = 0.0;
            for (String field : mapping.getSourceFields()) {
                String value = getSourceValue(row, field);
                if (value != null) {
                    try {
                        sum += Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        log.debug("Non-numeric value in sum: {} = {}", field, value);
                    }
                }
            }
            return String.valueOf(sum);
            
        } else if ("concat".equalsIgnoreCase(mapping.getOperation())) {
            String delimiter = mapping.getDelimiter() != null ? mapping.getDelimiter() : "";
            StringBuilder result = new StringBuilder();
            
            for (String field : mapping.getSourceFields()) {
                String value = getSourceValue(row, field);
                if (value != null) {
                    if (result.length() > 0) {
                        result.append(delimiter);
                    }
                    result.append(value);
                }
            }
            return result.toString();
        }
        
        return mapping.getFallback();
    }
    
    /**
     * Handle conditional transformations
     */
    private String handleConditional(Map<String, Object> row, EnhancedFieldMapping mapping) {
        if (mapping.getConditions() == null || mapping.getConditions().isEmpty()) {
            return mapping.getFallback();
        }

        var mainCondition = mapping.getConditions().get(0);
        String condition = mainCondition.getCondition();
        String thenValue = mainCondition.getThenValue();
        String elseValue = mainCondition.getElseValue();

        if (condition != null && !condition.isEmpty() && evaluateExpression(condition, row)) {
            return resolveValue(thenValue, row, thenValue);
        }

        if (elseValue != null && !elseValue.isEmpty()) {
            return resolveValue(elseValue, row, elseValue);
        }

        return mapping.getFallback();
    }

    /**
     * Evaluate logical expressions
     */
    private boolean evaluateExpression(String expression, Map<String, Object> row) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        for (String orPart : expression.split("\\|\\|")) {
            boolean andResult = true;
            
            for (String cond : orPart.split("&&")) {
                cond = cond.trim();
                
                boolean negation = false;
                if (cond.startsWith("!")) {
                    negation = true;
                    cond = cond.substring(1).trim();
                }
                
                var matcher = java.util.regex.Pattern
                    .compile("([^!=<>()\\s]+)\\s*(==|=|!=|>=|<=|<|>)\\s*('([^']*)'|\"([^\"]*)\"|[^\\s]+)")
                    .matcher(cond);
                    
                if (!matcher.matches()) {
                    andResult = false;
                    break;
                }
                
                String field = matcher.group(1);
                String op = matcher.group(2);
                String val = matcher.group(4) != null ? matcher.group(4) 
                           : matcher.group(5) != null ? matcher.group(5) 
                           : matcher.group(3);
                
                String fieldVal = getSourceValue(row, field);
                boolean thisResult = evaluateCondition(fieldVal, op, val);
                
                if (negation) {
                    thisResult = !thisResult;
                }
                
                if (!thisResult) {
                    andResult = false;
                    break;
                }
            }
            
            if (andResult) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Evaluate individual condition
     */
    private boolean evaluateCondition(String fieldVal, String op, String val) {
        switch (op) {
            case "=", "==":
                if ("null".equals(val)) {
                    return fieldVal == null;
                } else {
                    return fieldVal != null && fieldVal.equals(val);
                }
            case "!=":
                if ("null".equals(val)) {
                    return fieldVal != null;
                } else {
                    return fieldVal == null || !fieldVal.equals(val);
                }
            case "<", ">", "<=", ">=":
                return compareNumeric(fieldVal, val, op);
            default:
                return false;
        }
    }

    /**
     * Compare numeric values
     */
    private boolean compareNumeric(String fieldVal, String val, String op) {
        try {
            double fv = fieldVal != null ? Double.parseDouble(fieldVal) : 0;
            double vl = Double.parseDouble(val);
            
            return switch (op) {
                case "<" -> fv < vl;
                case ">" -> fv > vl;
                case "<=" -> fv <= vl;
                case ">=" -> fv >= vl;
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve value with case-insensitive lookup
     */
    private String resolveValue(String sourceField, Map<String, Object> row, String defaultValue) {
        if (sourceField == null || sourceField.isEmpty()) {
            return defaultValue;
        }
        
        Object value = row.get(sourceField);
        if (value != null) {
            return value.toString();
        }
        
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (sourceField.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() != null ? entry.getValue().toString() : defaultValue;
            }
        }
        
        return defaultValue;
    }
}
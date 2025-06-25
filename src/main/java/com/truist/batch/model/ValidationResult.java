package com.truist.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> duplicatePositions = new ArrayList<>();
    private List<String> duplicateFieldNames = new ArrayList<>();
    private List<String> missingRequiredFields = new ArrayList<>();
    private List<String> conflictingFields = new ArrayList<>();
    
    /**
     * Constructor for simple validation with just valid flag
     */
    public ValidationResult(boolean valid) {
        this.valid = valid;
    }
    
    /**
     * Constructor for validation with errors
     */
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    /**
     * Add an error to the validation result
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        this.valid = false;
    }
    
    /**
     * Add a warning to the validation result
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
    
    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Get total count of issues (errors + warnings)
     */
    public int getTotalIssueCount() {
        int errorCount = errors != null ? errors.size() : 0;
        int warningCount = warnings != null ? warnings.size() : 0;
        return errorCount + warningCount;
    }
    
    /**
     * Get summary message
     */
    public String getSummary() {
        if (valid && !hasWarnings()) {
            return "Validation passed with no issues";
        } else if (valid && hasWarnings()) {
            return String.format("Validation passed with %d warnings", warnings.size());
        } else {
            return String.format("Validation failed with %d errors and %d warnings", 
                               errors.size(), warnings.size());
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("valid=").append(valid);
        sb.append(", errors=").append(errors != null ? errors.size() : 0);
        sb.append(", warnings=").append(warnings != null ? warnings.size() : 0);
        sb.append('}');
        return sb.toString();
    }
}
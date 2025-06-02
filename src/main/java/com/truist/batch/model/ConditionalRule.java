package com.truist.batch.model;

import java.io.Serializable;

import lombok.Data;

/**
 * Represents a single conditional rule (if-then-else)
 */
@Data
public class ConditionalRule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Condition expression
     * Example: "account_status = 'ACTIVE'", "balance > 0"
     */
    private String condition;
    
    /**
     * Value or field name to use if condition is true
     * Example: "A", "ACTIVE_BALANCE_FIELD"
     */
    private String thenValue;
    
    /**
     * Value or field name to use if condition is false
     * Example: "I", "INACTIVE_BALANCE_FIELD"
     */
    private String elseValue;
}

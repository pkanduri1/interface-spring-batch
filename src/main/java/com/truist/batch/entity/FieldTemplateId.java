package com.truist.batch.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FieldTemplateId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String fileType;
    private String transactionType;
    private String fieldName;
    
    @Override
    public String toString() {
        return String.format("%s-%s-%s", fileType, transactionType, fieldName);
    }
}
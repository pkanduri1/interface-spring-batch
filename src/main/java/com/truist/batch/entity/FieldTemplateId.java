package com.truist.batch.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldTemplateId implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Must match the @Id fields in FieldTemplateEntity exactly
    private String fileType;
    private String transactionType;
    private String fieldName;
    
    // Override equals and hashCode for proper composite key handling
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldTemplateId that = (FieldTemplateId) o;
        return Objects.equals(fileType, that.fileType) &&
               Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(fieldName, that.fieldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fileType, transactionType, fieldName);
    }
    
    @Override
    public String toString() {
        return String.format("FieldTemplateId{fileType='%s', transactionType='%s', fieldName='%s'}", 
                           fileType, transactionType, fieldName);
    }
}
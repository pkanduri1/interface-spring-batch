package com.truist.batch.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "field_templates")
@IdClass(FieldTemplateId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FieldTemplateEntity {
    
    // PRIMARY KEY FIELDS - Must match database exactly
    @Id
    @Column(name = "file_type", length = 10, nullable = false)
    private String fileType;  // 'p327'
    
    @Id  
    @Column(name = "transaction_type", length = 10, nullable = false)
    private String transactionType;  // 'default', '200', '900'
    
    @Id
    @Column(name = "field_name", length = 50, nullable = false)
    private String fieldName;  // 'ACCT-NUM'
    
    // NON-KEY FIELDS
    @Column(name = "target_position", nullable = false)
    private Integer targetPosition;  // 1, 2, 3, 4... (sequential)
    
    @Column(name = "length")
    private Integer length;  // 18
    
    @Column(name = "data_type", length = 20)
    private String dataType;  // 'String'
    
    @Column(name = "format", length = 50)
    private String format;  // '+9(12)V9(6)'
    
    @Column(name = "required", columnDefinition = "CHAR(1)", nullable = false)
    private String required = "N";  // 'Y'/'N'
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "enabled", columnDefinition = "CHAR(1)", nullable = false)
    private String enabled = "Y";
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (version == null) {
            version = 1;
        }
        if (enabled == null) {
            enabled = "Y";
        }
        if (required == null) {
            required = "N";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        if (version != null) {
            version++;
        }
    }
}
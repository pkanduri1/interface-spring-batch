package com.truist.batch.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_type_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeTemplateEntity {
    
    @Id
    @Column(name = "file_type", length = 10)
    private String fileType;  // 'p327'
    
    @Column(name = "description", length = 200)
    private String description;  // 'Consumer Lending Interface'
    
    @Column(name = "total_fields")
    private Integer totalFields;  // 251
    
    @Column(name = "record_length")
    private Integer recordLength;  // Total fixed-width record length
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by")
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    private Integer version = 1;
    
    @Column(length = 1)
    private String enabled = "Y";
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        version++;
    }
}
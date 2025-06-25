package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Entity
@Table(name = "field_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@IdClass(FieldTemplateId.class)
public class FieldTemplateEntity {
    
    @Id
    @Column(name = "file_type", length = 10, nullable = false)
    private String fileType;
    
    @Id
    @Column(name = "transaction_type", length = 10, nullable = false)
    private String transactionType;
    
    @Id
    @Column(name = "field_name", length = 50, nullable = false)
    private String fieldName;
    
    @Column(name = "target_position", nullable = false)
    private Integer targetPosition;
    
    @Column(name = "length")
    private Integer length;
    
    @Column(name = "data_type", length = 20)
    private String dataType;
    
    @Column(name = "format", length = 50)
    private String format;
    
    @Column(name = "required", length = 1)
    private String required = "N";
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_date")
    private Date modifiedDate;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "enabled", length = 1)
    private String enabled = "Y";
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Date();
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
        modifiedDate = new Date();
        if (version != null) {
            version = version + 1;
        }
    }
}
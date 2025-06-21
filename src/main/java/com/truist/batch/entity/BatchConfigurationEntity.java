package com.truist.batch.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "batch_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BatchConfigurationEntity {
    
    @Id
    private String id;
    
    @Column(name = "source_system", nullable = false)
    private String sourceSystem;
    
    @Column(name = "job_name", nullable = false)
    private String jobName;
    
    @Column(name = "transaction_type")
    private String transactionType = "200";
    
    private String description;
    
    @Lob
    @Column(name = "configuration_json", nullable = false)
    private String configurationJson;
    
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
        if (id == null) {
            id = sourceSystem + "-" + jobName + "-" + transactionType + "-" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        version++;
    }
}

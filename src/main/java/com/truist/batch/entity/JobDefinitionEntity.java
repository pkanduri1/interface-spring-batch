package com.truist.batch.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDefinitionEntity {
    
    @Id
    private String id;
    
    @Column(name = "source_system_id", nullable = false)
    private String sourceSystemId;
    
    @Column(name = "job_name", nullable = false)
    private String jobName;
    
    private String description;
    
    @Column(name = "input_path")
    private String inputPath;
    
    @Column(name = "output_path")
    private String outputPath;
    
    @Lob
    @Column(name = "query_sql")
    private String querySql;
    
    @Column(length = 1)
    private String enabled = "Y";
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "transaction_types")
    private String transactionTypes; // Comma-separated: "200,300,900"
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (id == null) {
            id = sourceSystemId + "-" + jobName;
        }
    }
}

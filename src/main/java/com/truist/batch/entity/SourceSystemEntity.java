package com.truist.batch.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "source_systems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceSystemEntity {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private SystemType type;
    
    private String description;
    
    @Column(name = "connection_string")
    private String connectionString;
    
    @Column(length = 1)
    private String enabled = "Y";
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "job_count")
    private Integer jobCount = 0;
    
    public enum SystemType {
        ORACLE, SQLSERVER, FILE, API
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}


package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_audit")
@Data
public class ConfigurationAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;
    
    @Column(name = "config_id")
    private String configId;
    
    private String action;
    
    @Lob
    @Column(name = "old_value")
    private String oldValue;
    
    @Lob
    @Column(name = "new_value")
    private String newValue;
    
    @Column(name = "changed_by")
    private String changedBy;
    
    
    @Column(name = "change_reason")
    private String reason;
    
    @Column(name = "change_date")
    private LocalDateTime changeDate;
    
    @PrePersist
    protected void onCreate() {
        changeDate = LocalDateTime.now();
    }
}
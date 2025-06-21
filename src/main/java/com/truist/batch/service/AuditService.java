package com.truist.batch.service;

import com.truist.batch.entity.ConfigurationAuditEntity;
import com.truist.batch.repository.ConfigurationAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final ConfigurationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void logCreate(String configId, Object newValue, String changedBy, String reason) {
        ConfigurationAuditEntity audit = new ConfigurationAuditEntity();
        audit.setConfigId(configId);
        audit.setAction("CREATE");
        audit.setNewValue(toJson(newValue));
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        auditRepository.save(audit);
    }
    
    @Transactional
    public void logUpdate(String configId, Object oldValue, Object newValue, String changedBy, String reason) {
        ConfigurationAuditEntity audit = new ConfigurationAuditEntity();
        audit.setConfigId(configId);
        audit.setAction("UPDATE");
        audit.setOldValue(toJson(oldValue));
        audit.setNewValue(toJson(newValue));
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        auditRepository.save(audit);
    }
    
    @Transactional
    public void logDelete(String configId, Object oldValue, String changedBy, String reason) {
        ConfigurationAuditEntity audit = new ConfigurationAuditEntity();
        audit.setConfigId(configId);
        audit.setAction("DELETE");
        audit.setOldValue(toJson(oldValue));
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        auditRepository.save(audit);
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj != null ? obj.toString() : null;
        }
    }
}

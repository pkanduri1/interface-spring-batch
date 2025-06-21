package com.truist.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.ConfigurationAuditEntity;

@Repository
public interface ConfigurationAuditRepository extends JpaRepository<ConfigurationAuditEntity, Long> {
    
    List<ConfigurationAuditEntity> findByConfigIdOrderByChangeDateDesc(String configId);
    
    @Query("SELECT a FROM ConfigurationAuditEntity a WHERE a.changedBy = :user ORDER BY a.changeDate DESC")
    List<ConfigurationAuditEntity> findByChangedByOrderByChangeDateDesc(@Param("user") String user);
    
    @Query("SELECT a FROM ConfigurationAuditEntity a WHERE a.changeDate >= :fromDate ORDER BY a.changeDate DESC")
    List<ConfigurationAuditEntity> findRecentChanges(@Param("fromDate") java.time.LocalDateTime fromDate);
}
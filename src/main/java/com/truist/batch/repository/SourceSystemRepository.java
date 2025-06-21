package com.truist.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.SourceSystemEntity;

@Repository
public interface SourceSystemRepository extends JpaRepository<SourceSystemEntity, String> {
    
    @Query("SELECT s FROM SourceSystemEntity s WHERE s.enabled = 'Y'")
    List<SourceSystemEntity> findAllEnabled();
    
    List<SourceSystemEntity> findByType(SourceSystemEntity.SystemType type);
}

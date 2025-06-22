package com.truist.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.FileTypeTemplateEntity;

@Repository
public interface FileTypeTemplateRepository extends JpaRepository<FileTypeTemplateEntity, String> {
    
    List<FileTypeTemplateEntity> findByEnabledOrderByFileType(String enabled);
    
    Optional<FileTypeTemplateEntity> findByFileTypeAndEnabled(String fileType, String enabled);
    
    @Query("SELECT f FROM FileTypeTemplateEntity f WHERE f.enabled = 'Y'")
    List<FileTypeTemplateEntity> findAllEnabled();
    
    @Query("SELECT COUNT(f) FROM FileTypeTemplateEntity f WHERE f.enabled = 'Y'")
    long countEnabled();
}

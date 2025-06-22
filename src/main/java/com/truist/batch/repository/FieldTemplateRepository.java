package com.truist.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.FieldTemplateEntity;
import com.truist.batch.entity.FieldTemplateId;

@Repository
public interface FieldTemplateRepository extends JpaRepository<FieldTemplateEntity, FieldTemplateId> {
    
    List<FieldTemplateEntity> findByFileTypeAndTransactionTypeOrderByTargetPosition(String fileType, String transactionType);
    
    List<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(String fileType, String transactionType, String enabled);
    
    Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndFieldName(String fileType, String transactionType, String fieldName);
    
    @Query("SELECT COUNT(f) FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType AND f.enabled = 'Y'")
    long countEnabledFieldsByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT MAX(f.targetPosition) FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType")
    Integer getMaxTargetPositionByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT SUM(f.length) FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType AND f.enabled = 'Y'")
    Integer getTotalRecordLengthByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT f FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType AND f.required = 'Y' AND f.enabled = 'Y'")
    List<FieldTemplateEntity> findRequiredFieldsByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT DISTINCT f.fileType FROM FieldTemplateEntity f WHERE f.enabled = 'Y'")
    List<String> findAllEnabledFileTypes();
    
    @Query("SELECT DISTINCT f.transactionType FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.enabled = 'Y'")
    List<String> findTransactionTypesByFileType(@Param("fileType") String fileType);

}

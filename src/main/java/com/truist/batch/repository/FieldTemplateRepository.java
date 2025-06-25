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
    
    
    /**
     * Find by composite key
     */
    Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndFieldName(
        String fileType, String transactionType, String fieldName);
    
    /**
     * Find by position (to check for conflicts)
     */
    Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndTargetPosition(
        String fileType, String transactionType, Integer targetPosition);
    
    /**
     * Find all fields for a file type and transaction type (ordered by position)
     */
    List<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(
        String fileType, String transactionType, String enabled);
    
    /**
     * Find all transaction types for a file type
     */
    @Query("SELECT DISTINCT ft.transactionType FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.enabled = 'Y'")
    List<String> findTransactionTypesByFileType(@Param("fileType") String fileType);
    
    /**
     * Find all fields for a file type (across all transaction types)
     */
    List<FieldTemplateEntity> findByFileTypeAndEnabledOrderByTransactionTypeAscTargetPositionAsc(
        String fileType, String enabled);
    
    /**
     * Count fields for a file type and transaction type
     */
    Long countByFileTypeAndTransactionTypeAndEnabled(String fileType, String transactionType, String enabled);
    
    /**
     * Find fields by file type with pagination support
     */
    @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.enabled = :enabled ORDER BY ft.transactionType, ft.targetPosition")
    List<FieldTemplateEntity> findByFileTypeAndEnabledOrderByTransactionTypeAndPosition(
        @Param("fileType") String fileType, @Param("enabled") String enabled);
    
    /**
     * Delete all fields for a file type and transaction type (for cleanup)
     */
    void deleteByFileTypeAndTransactionType(String fileType, String transactionType);
    
    /**
     * Find maximum position for a file type and transaction type
     */
    @Query("SELECT COALESCE(MAX(ft.targetPosition), 0) FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.transactionType = :transactionType AND ft.enabled = 'Y'")
    Integer findMaxPositionByFileTypeAndTransactionType(
        @Param("fileType") String fileType, @Param("transactionType") String transactionType);


}

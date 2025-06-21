package com.truist.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.JobDefinitionEntity;

@Repository
public interface JobDefinitionRepository extends JpaRepository<JobDefinitionEntity, String> {
    
    List<JobDefinitionEntity> findBySourceSystemId(String sourceSystemId);
    
    Optional<JobDefinitionEntity> findBySourceSystemIdAndJobName(String sourceSystemId, String jobName);
    
    @Query("SELECT j FROM JobDefinitionEntity j WHERE j.enabled = 'Y'")
    List<JobDefinitionEntity> findAllEnabled();
    
    @Query("SELECT j FROM JobDefinitionEntity j WHERE j.transactionTypes LIKE %:transactionType%")
    List<JobDefinitionEntity> findByTransactionType(@Param("transactionType") String transactionType);
}

package com.truist.batch.partition;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FileConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GenericPartitioner implements Partitioner {

    private final YamlMappingService mappingService;
    private final Map<String, Object> systemConfig;
    private final Map<String, Object> jobConfig;
    private final String sourceSystem;
    private final String jobName;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        try {
            // Extract files list from job configuration
            @SuppressWarnings("unchecked")
            var files = (java.util.List<Map<String, Object>>) jobConfig.get("files");
            
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("No files configured for job: " + jobName);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            int partitionIndex = 0;

            // ✅ FIX: Process each file config exactly once - no YAML scanning
            for (Map<String, Object> fileMap : files) {
                FileConfig fileConfig = objectMapper.convertValue(fileMap, FileConfig.class);
                
                // Set dynamic context
                fileConfig.setSourceSystem(sourceSystem);
                fileConfig.setJobName(jobName);
                
                // Use the explicitly configured transaction type
                String transactionType = fileConfig.getTransactionType();
                if (transactionType == null || transactionType.isEmpty()) {
                    transactionType = "default";
                }

                // Create partition key
                String partitionKey = String.format("partition_%d_%s_%s", 
                        partitionIndex++, jobName, transactionType);

                // Create execution context for this partition
                ExecutionContext executionContext = new ExecutionContext();
                executionContext.put("fileConfig", fileConfig);
                executionContext.put("sourceSystem", sourceSystem);
                executionContext.put("jobName", jobName);
                executionContext.put("transactionType", transactionType);
                
                partitions.put(partitionKey, executionContext);

                log.info("📝 Created partition: {} for txnType: {}, outputPath: {}", 
                        partitionKey, transactionType, fileConfig.getParams().get("outputPath"));
            }

            log.info("🎯 Total partitions created: {} for job: {}.{}", 
                    partitions.size(), sourceSystem, jobName);
            
            return partitions;

        } catch (Exception e) {
            log.error("❌ Failed to create partitions for job: {}.{}", sourceSystem, jobName, e);
            throw new RuntimeException("Partitioning failed", e);
        }
    }
}
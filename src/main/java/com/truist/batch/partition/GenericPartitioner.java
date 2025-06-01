package com.truist.batch.partition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.YamlMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor  // ← This generates the constructor automatically
public class GenericPartitioner implements Partitioner {

    // ✅ All fields are final - Lombok will generate constructor for these in order
    private final YamlMappingService mappingService;
    private final Map<String, Object> systemConfig;
    private final Map<String, Object> jobConfig;
    private final String sourceSystem;
    private final String jobName;

    // ❌ NO MANUAL CONSTRUCTOR NEEDED - Lombok generates this:
    // public GenericPartitioner(YamlMappingService mappingService, 
    //                          Map<String, Object> systemConfig, 
    //                          Map<String, Object> jobConfig,
    //                          String sourceSystem,
    //                          String jobName) { ... }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        try {
            // Extract files list from job configuration
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) jobConfig.get("files");
            
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("No files configured for job: " + jobName);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            int partitionIndex = 0;

            for (Map<String, Object> fileMap : files) {
                // Convert map to FileConfig object
                FileConfig fileConfig = objectMapper.convertValue(fileMap, FileConfig.class);
                
                // ✅ SET DYNAMIC CONTEXT
                fileConfig.setSourceSystem(sourceSystem);
                fileConfig.setJobName(jobName);
                
                // Extract transaction type if present
                String transactionType = fileConfig.getTransactionType();
                if (transactionType == null || transactionType.isEmpty()) {
                    transactionType = "default";
                }
                fileConfig.setTransactionType(transactionType);

                // 🔍 DYNAMIC TEMPLATE PATH CONSTRUCTION
                String templatePath = fileConfig.getTemplate(); // Auto-constructs: jobName/sourceSystem/jobName.yml
                log.info("🚀 Dynamic template path constructed: {} for job: {}, sourceSystem: {}, txnType: {}", 
                        templatePath, jobName, sourceSystem, transactionType);

                // Load YAML mapping to get all transaction types for this template
                List<YamlMapping> allMappings = mappingService.loadYamlMappings(templatePath);
                log.info("🔍 DEBUG: loadYamlMappings returned {} mappings", allMappings.size());
                // Create partitions for each transaction type found in the YAML
                for (YamlMapping mapping : allMappings) {
                    String mappingTxnType = mapping.getTransactionType();
                    if (mappingTxnType == null || mappingTxnType.isEmpty()) {
                        mappingTxnType = "default";
                    }

                    // Create partition key
                    String partitionKey = String.format("partition_%d_%s_%s", 
                            partitionIndex++, jobName, mappingTxnType);

                    // Create execution context for this partition
                    ExecutionContext executionContext = new ExecutionContext();
                    
                    // Clone fileConfig for this specific transaction type
                    FileConfig partitionFileConfig = cloneFileConfig(fileConfig);
                    partitionFileConfig.setTransactionType(mappingTxnType);
                    
                    // Add to execution context
                    executionContext.put("fileConfig", partitionFileConfig);
                    executionContext.put("sourceSystem", sourceSystem);
                    executionContext.put("jobName", jobName);
                    executionContext.put("transactionType", mappingTxnType);
                    
                    partitions.put(partitionKey, executionContext);

                    log.info("📝 Created partition: {} for template: {}, txnType: {}", 
                            partitionKey, templatePath, mappingTxnType);
                }
            }

            log.info("🎯 Total partitions created: {} for job: {}.{}", 
                    partitions.size(), sourceSystem, jobName);
            
            return partitions;

        } catch (Exception e) {
            log.error("❌ Failed to create partitions for job: {}.{}", sourceSystem, jobName, e);
            throw new RuntimeException("Partitioning failed", e);
        }
    }

    /**
     * Helper method to clone FileConfig for different transaction types
     */
    private FileConfig cloneFileConfig(FileConfig original) {
        FileConfig clone = new FileConfig();
        clone.setInputPath(original.getInputPath());
        clone.setTransactionTypes(original.getTransactionTypes());
        clone.setTemplate(original.getTemplate()); // Will use the same auto-constructed path
        clone.setTarget(original.getTarget());
        clone.setParams(original.getParams());
        clone.setSourceSystem(original.getSourceSystem());
        clone.setJobName(original.getJobName());
        return clone;
    }
}
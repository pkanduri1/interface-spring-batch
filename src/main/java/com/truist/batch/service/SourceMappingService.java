package com.truist.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.EnhancedFieldMapping;
import com.truist.batch.model.MappingType;
import com.truist.batch.model.SourceTargetMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading and caching source-specific field mappings.
 */
@Service
@Slf4j
public class SourceMappingService {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, SourceTargetMapping> cache = new ConcurrentHashMap<>();
    
    /**
     * Get field mapping for specific source, target, and transaction type
     */
    public EnhancedFieldMapping getFieldMapping(String sourceSystem, String targetName, 
                                               String fieldName, String transactionType) {
        SourceTargetMapping mapping = getSourceMapping(sourceSystem, targetName);
        
        // Priority: transaction-specific > general > defaults
        EnhancedFieldMapping fieldMapping = null;
        
        // Check transaction-specific mappings first
        if (transactionType != null && mapping.getTransactionMappings() != null) {
            Map<String, EnhancedFieldMapping> txnMappings = mapping.getTransactionMappings().get(transactionType);
            if (txnMappings != null) {
                fieldMapping = txnMappings.get(fieldName);
            }
        }
        
        // Check general mappings
        if (fieldMapping == null && mapping.getMappings() != null) {
            Map<String, EnhancedFieldMapping> generalMappings = mapping.getMappings().get("default");
            if (generalMappings != null) {
                fieldMapping = generalMappings.get(fieldName);
            }
        }
        
        // Check defaults
        if (fieldMapping == null && mapping.getDefaults() != null) {
            fieldMapping = mapping.getDefaults().get(fieldName);
        }
        
        // Return default constant mapping if none found
        if (fieldMapping == null) {
            fieldMapping = createDefaultMapping();
        }
        
        return fieldMapping;
    }
    
    /**
     * Load source mapping with caching
     */
    public SourceTargetMapping getSourceMapping(String sourceSystem, String targetName) {
        String key = sourceSystem + ":" + targetName;
        return cache.computeIfAbsent(key, k -> loadSourceMapping(sourceSystem, targetName));
    }
    
    /**
     * Load source mapping from YAML file
     */
    private SourceTargetMapping loadSourceMapping(String sourceSystem, String targetName) {
        try {
            String mappingPath = String.format("mappings/%s/%s-mapping.yml", sourceSystem, targetName);
            ClassPathResource resource = new ClassPathResource(mappingPath);
            
            if (!resource.exists()) {
                log.warn("Source mapping not found: {}, using defaults", mappingPath);
                return createEmptyMapping(sourceSystem, targetName);
            }
            
            log.debug("Loading source mapping: {}", mappingPath);
            
            SourceTargetMapping mapping = yamlMapper.readValue(
                resource.getInputStream(), 
                SourceTargetMapping.class
            );
            
            log.info("✅ Loaded source mapping: {}/{} ({} field mappings)", 
                sourceSystem, targetName, countMappings(mapping));
            
            return mapping;
            
        } catch (IOException e) {
            log.error("Failed to load source mapping: {}/{}", sourceSystem, targetName, e);
            return createEmptyMapping(sourceSystem, targetName);
        }
    }
    
    /**
     * Create empty mapping as fallback
     */
    private SourceTargetMapping createEmptyMapping(String sourceSystem, String targetName) {
        SourceTargetMapping mapping = new SourceTargetMapping();
        mapping.setSourceSystem(sourceSystem);
        mapping.setTargetName(targetName);
        mapping.setDescription("Empty mapping (fallback)");
        return mapping;
    }
    
    /**
     * Create default field mapping
     */
    private EnhancedFieldMapping createDefaultMapping() {
        EnhancedFieldMapping mapping = new EnhancedFieldMapping();
        mapping.setType(MappingType.CONSTANT);
        mapping.setValue(" ");
        mapping.setFallback(" ");
        return mapping;
    }
    
    /**
     * Count total mappings for logging
     */
    private int countMappings(SourceTargetMapping mapping) {
        int count = 0;
        if (mapping.getDefaults() != null) count += mapping.getDefaults().size();
        if (mapping.getMappings() != null) {
            count += mapping.getMappings().values().stream().mapToInt(Map::size).sum();
        }
        if (mapping.getTransactionMappings() != null) {
            count += mapping.getTransactionMappings().values().stream().mapToInt(Map::size).sum();
        }
        return count;
    }
    
    /**
     * Clear cache (for testing)
     */
    public void clearCache() {
        cache.clear();
        log.info("Source mapping cache cleared");
    }
}
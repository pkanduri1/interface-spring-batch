package com.truist.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.TargetDefinition;
import com.truist.batch.model.TargetField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading and caching target definitions from YAML files.
 * Provides centralized target schema management.
 */
@Service
@Slf4j
public class TargetDefinitionService {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, TargetDefinition> cache = new ConcurrentHashMap<>();
    
    /**
     * Load target definition with caching
     */
    public TargetDefinition getTargetDefinition(String targetName) {
        return cache.computeIfAbsent(targetName, this::loadTargetDefinition);
    }
    
    /**
     * Load target definition from YAML file
     */
    private TargetDefinition loadTargetDefinition(String targetName) {
        try {
            String targetPath = String.format("targets/%s.yml", targetName);
            ClassPathResource resource = new ClassPathResource(targetPath);
            
            if (!resource.exists()) {
                throw new IllegalArgumentException("Target definition not found: " + targetPath);
            }
            
            log.debug("Loading target definition: {}", targetPath);
            
            TargetDefinition definition = yamlMapper.readValue(
                resource.getInputStream(), 
                TargetDefinition.class
            );
            
            validateTargetDefinition(definition);
            log.info("✅ Loaded target definition: {} ({} fields)", 
                targetName, definition.getFields().size());
            
            return definition;
            
        } catch (IOException e) {
            log.error("Failed to load target definition: {}", targetName, e);
            throw new RuntimeException("Failed to load target definition", e);
        }
    }
    
    /**
     * Get specific field definition
     */
    public TargetField getTargetField(String targetName, String fieldName) {
        TargetDefinition definition = getTargetDefinition(targetName);
        return definition.getField(fieldName);
    }
    
    /**
     * Validate target definition consistency
     */
    private void validateTargetDefinition(TargetDefinition definition) {
        if (definition.getFields() == null || definition.getFields().isEmpty()) {
            throw new IllegalArgumentException("Target definition must have fields");
        }
        
        // Validate position sequence
        definition.getFields().stream()
            .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
            .forEach(field -> {
                if (field.getPosition() == null || field.getPosition() < 1) {
                    throw new IllegalArgumentException("Invalid field position: " + field.getName());
                }
                if (field.getLength() == null || field.getLength() < 1) {
                    throw new IllegalArgumentException("Invalid field length: " + field.getName());
                }
            });
    }
    
    /**
     * Clear cache (for testing)
     */
    public void clearCache() {
        cache.clear();
        log.info("Target definition cache cleared");
    }
}
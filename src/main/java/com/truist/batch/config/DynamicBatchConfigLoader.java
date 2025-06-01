package com.truist.batch.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DynamicBatchConfigLoader {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    @Autowired
    private Environment environment;
    
    // Thread-safe cache for loaded configurations
    private final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();
    
    /**
     * Loads and caches source system configuration with property resolution
     * Thread-safe for parallel execution
     */
    public Map<String, Object> loadSourceSystemConfig(String sourceSystem) {
        // Check cache first
        return configCache.computeIfAbsent(sourceSystem, this::loadConfigFromFile);
    }
    
    private Map<String, Object> loadConfigFromFile(String sourceSystem) {
        try {
            String configFile = String.format("batch-sources/%s-batch-props.yml", sourceSystem);
            ClassPathResource resource = new ClassPathResource(configFile);
            
            if (!resource.exists()) {
                throw new IllegalArgumentException("Configuration file not found: " + configFile);
            }
            
            log.info("Loading configuration from: {} (Thread: {})", configFile, Thread.currentThread().getName());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> config = yamlMapper.readValue(resource.getInputStream(), Map.class);
            
            log.debug("🔍 Raw config before property resolution: {}", config);
            
            // ✅ RESOLVE PROPERTY PLACEHOLDERS
            Map<String, Object> resolvedConfig = resolveProperties(config);
            
            log.debug("🔍 Resolved config after property resolution: {}", resolvedConfig);
            log.info("Successfully loaded and cached configuration for source system: {}", sourceSystem);
            return resolvedConfig;
            
        } catch (IOException e) {
            log.error("Failed to load configuration for source system: {}", sourceSystem, e);
            throw new RuntimeException("Failed to load batch configuration", e);
        }
    }
    
    /**
     * Recursively resolves ${...} property placeholders in configuration maps
     * Enhanced to handle nested maps and lists properly
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveProperties(Map<String, Object> config) {
        Map<String, Object> resolved = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                // Resolve property placeholders in string values
                String stringValue = (String) value;
                String resolvedValue = environment.resolvePlaceholders(stringValue);
                resolved.put(key, resolvedValue);
                
                // Log property resolution for debugging
                if (!stringValue.equals(resolvedValue)) {
                    log.debug("🔧 Resolved property: {} = {} → {}", key, stringValue, resolvedValue);
                }
            } else if (value instanceof Map) {
                // Recursively resolve nested maps
                resolved.put(key, resolveProperties((Map<String, Object>) value));
            } else if (value instanceof List) {
                // Handle lists that might contain maps with properties
                List<Object> originalList = (List<Object>) value;
                List<Object> resolvedList = new ArrayList<>();
                
                for (Object item : originalList) {
                    if (item instanceof Map) {
                        resolvedList.add(resolveProperties((Map<String, Object>) item));
                    } else if (item instanceof String) {
                        resolvedList.add(environment.resolvePlaceholders((String) item));
                    } else {
                        resolvedList.add(item);
                    }
                }
                resolved.put(key, resolvedList);
            } else {
                // Keep other types as-is
                resolved.put(key, value);
            }
        }
        
        return resolved;
    }
    
    public Map<String, Object> getSourceSystemConfig(String sourceSystem) {
        Map<String, Object> fullConfig = loadSourceSystemConfig(sourceSystem);
        
        // Navigate to batch.sources.{sourceSystem}
        @SuppressWarnings("unchecked")
        Map<String, Object> batchConfig = (Map<String, Object>) fullConfig.get("batch");
        if (batchConfig == null) {
            throw new IllegalArgumentException("No batch configuration found in " + sourceSystem + "-batch-props.yml");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sources = (Map<String, Object>) batchConfig.get("sources");
        if (sources == null) {
            throw new IllegalArgumentException("No sources configuration found in " + sourceSystem + "-batch-props.yml");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceConfig = (Map<String, Object>) sources.get(sourceSystem);
        if (sourceConfig == null) {
            throw new IllegalArgumentException("No configuration found for source system: " + sourceSystem);
        }
        
        return sourceConfig;
    }
    
    public Map<String, Object> getJobConfig(String sourceSystem, String jobName) {
        Map<String, Object> sourceConfig = getSourceSystemConfig(sourceSystem);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> jobs = (Map<String, Object>) sourceConfig.get("jobs");
        if (jobs == null) {
            throw new IllegalArgumentException("No jobs configuration found for source system: " + sourceSystem);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> jobConfig = (Map<String, Object>) jobs.get(jobName);
        if (jobConfig == null) {
            throw new IllegalArgumentException("No job configuration found for: " + sourceSystem + "." + jobName);
        }
        
        return jobConfig;
    }
    
    /**
     * Clear cache if needed (useful for testing or hot reloading)
     */
    public void clearCache() {
        configCache.clear();
        log.info("Configuration cache cleared");
    }
    
    /**
     * Get cached source systems for monitoring
     */
    public Set<String> getCachedSourceSystems() {
        return configCache.keySet();
    }
}
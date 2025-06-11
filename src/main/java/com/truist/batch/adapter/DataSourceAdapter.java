package com.truist.batch.adapter;

import java.util.Map;

import org.springframework.batch.item.ItemReader;

import com.truist.batch.model.FileConfig;

/**
 * Service Provider Interface (SPI) for pluggable data source adapters.
 * 
 * Implementations provide specialized readers for different source types:
 * - JDBC databases
 * - REST APIs  
 * - Kafka streams
 * - S3 files
 * - etc.
 * 
 * The adapter pattern allows adding new source types without modifying core framework code.
 */
public interface DataSourceAdapter {
    
    /**
     * Determines if this adapter can handle the specified format.
     * 
     * @param format The format identifier from FileConfig.params.format
     * @return true if this adapter supports the format
     */
    boolean supports(String format);
    
    /**
     * Creates an ItemReader for the specified configuration.
     * 
     * @param fileConfig Configuration containing connection details, queries, etc.
     * @return Configured ItemReader that yields Map<String, Object> records
     * @throws IllegalArgumentException if configuration is invalid for this adapter
     */
    ItemReader<Map<String, Object>> createReader(FileConfig fileConfig);
    
    /**
     * Gets the adapter name for logging and identification.
     * 
     * @return Human-readable adapter name
     */
    default String getAdapterName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Validates the configuration for this adapter.
     * Called during application startup to catch configuration errors early.
     * 
     * @param fileConfig Configuration to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    default void validateConfiguration(FileConfig fileConfig) {
        // Default implementation - no validation
        // Adapters can override to provide specific validation
    }
    
    /**
     * Gets the priority for this adapter when multiple adapters support the same format.
     * Higher values take precedence.
     * 
     * @return Priority value (default is 0)
     */
    default int getPriority() {
        return 0;
    }
}

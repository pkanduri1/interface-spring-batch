package com.truist.batch.adapter;

import java.util.Map;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import com.truist.batch.model.FileConfig;
import com.truist.batch.reader.RestApiReader;

import lombok.extern.slf4j.Slf4j;

/**
 * DataSourceAdapter implementation for REST API sources.
 * 
 * Supports reading data from HTTP/HTTPS REST endpoints that return JSON.
 * This is our first "new" source type beyond the existing JDBC/file support.
 * 
 * Configuration example:
 * format: rest
 * baseUrl: https://api.example.com
 * endpoint: /users
 * authToken: Bearer xyz123
 */
@Component
@Slf4j
public class RestApiDataSourceAdapter implements DataSourceAdapter {
    
    @Override
    public boolean supports(String format) {
        return "rest".equalsIgnoreCase(format) || 
               "api".equalsIgnoreCase(format) ||
               "http".equalsIgnoreCase(format) ||
               "https".equalsIgnoreCase(format);
    }
    
    @Override
    public ItemReader<Map<String, Object>> createReader(FileConfig fileConfig) {
        log.info("🌐 Creating REST API reader for endpoint: {}", 
            fileConfig.getParams().get("baseUrl") + fileConfig.getParams().get("endpoint"));
        
        return new RestApiReader(fileConfig);
    }
    
    @Override
    public void validateConfiguration(FileConfig fileConfig) {
        Map<String, String> params = fileConfig.getParams();
        
        // Validate required parameters
        String baseUrl = params.get("baseUrl");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("REST adapter requires 'baseUrl' parameter");
        }
        
        String endpoint = params.get("endpoint");
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("REST adapter requires 'endpoint' parameter");
        }
        
        // Validate URL format
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must start with http:// or https://");
        }
        
        // Validate timeout parameters if provided
        String timeout = params.get("timeout");
        if (timeout != null && !timeout.trim().isEmpty()) {
            try {
                int timeoutMs = Integer.parseInt(timeout);
                if (timeoutMs <= 0) {
                    throw new IllegalArgumentException("timeout must be positive, got: " + timeoutMs);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("timeout must be a valid integer, got: " + timeout);
            }
        }
        
        log.debug("✅ REST API configuration validation passed for: {}{}", baseUrl, endpoint);
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority for REST APIs
    }
}
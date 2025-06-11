package com.truist.batch.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemReader;

import com.truist.batch.model.FileConfig;

class RestApiDataSourceAdapterTest {

    private RestApiDataSourceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RestApiDataSourceAdapter();
    }

    @Test
    void testSupportsRestFormats() {
        assertTrue(adapter.supports("rest"));
        assertTrue(adapter.supports("api"));
        assertTrue(adapter.supports("http"));
        assertTrue(adapter.supports("https"));
        assertTrue(adapter.supports("REST")); // Case insensitive
        assertFalse(adapter.supports("jdbc"));
        assertFalse(adapter.supports("csv"));
    }

    @Test
    void testCreateReader() {
        // Setup file config
        FileConfig fileConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "rest");
        params.put("baseUrl", "https://api.example.com");
        params.put("endpoint", "/users");
        fileConfig.setParams(params);
        
        // Test reader creation
        ItemReader<Map<String, Object>> reader = adapter.createReader(fileConfig);
        assertNotNull(reader);
        assertTrue(reader instanceof com.truist.batch.reader.RestApiReader);
    }

    @Test
    void testValidateConfiguration() {
        // Valid configuration
        FileConfig validConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "rest");
        params.put("baseUrl", "https://api.example.com");
        params.put("endpoint", "/users");
        params.put("timeout", "30000");
        validConfig.setParams(params);
        
        // Should not throw exception
        assertDoesNotThrow(() -> adapter.validateConfiguration(validConfig));
    }

    @Test
    void testValidateConfigurationMissingBaseUrl() {
        // Invalid configuration - missing baseUrl
        FileConfig invalidConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "rest");
        params.put("endpoint", "/users");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("REST adapter requires 'baseUrl' parameter"));
    }

    @Test
    void testValidateConfigurationMissingEndpoint() {
        // Invalid configuration - missing endpoint
        FileConfig invalidConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "rest");
        params.put("baseUrl", "https://api.example.com");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("REST adapter requires 'endpoint' parameter"));
    }

    @Test
    void testValidateConfigurationInvalidBaseUrl() {
        // Invalid configuration - invalid baseUrl format
        FileConfig invalidConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "rest");
        params.put("baseUrl", "invalid-url");
        params.put("endpoint", "/users");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("baseUrl must start with http:// or https://"));
    }

    @Test
    void testValidateConfigurationInvalidTimeout() {
        // Invalid configuration - invalid timeout
        FileConfig invalidConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "rest");
        params.put("baseUrl", "https://api.example.com");
        params.put("endpoint", "/users");
        params.put("timeout", "invalid");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("timeout must be a valid integer"));
    }

    @Test
    void testGetPriority() {
        assertEquals(50, adapter.getPriority());
    }

    @Test
    void testGetAdapterName() {
        assertEquals("RestApiDataSourceAdapter", adapter.getAdapterName());
    }
}
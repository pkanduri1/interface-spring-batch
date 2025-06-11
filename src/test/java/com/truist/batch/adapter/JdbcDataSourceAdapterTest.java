package com.truist.batch.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemReader;

import com.truist.batch.model.FileConfig;

@ExtendWith(MockitoExtension.class)
class JdbcDataSourceAdapterTest {

    @Mock
    private DataSource dataSource;
    
    private JdbcDataSourceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JdbcDataSourceAdapter(dataSource);
    }

    @Test
    void testSupportsJdbcFormats() {
        assertTrue(adapter.supports("jdbc"));
        assertTrue(adapter.supports("database"));
        assertTrue(adapter.supports("sql"));
        assertTrue(adapter.supports("JDBC")); // Case insensitive
        assertFalse(adapter.supports("rest"));
        assertFalse(adapter.supports("csv"));
    }

    @Test
    void testCreateReader() {
        // Setup file config
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTarget("TEST_TABLE");
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        params.put("fetchSize", "500");
        params.put("pageSize", "1000");
        fileConfig.setParams(params);
        
        // Test reader creation
        ItemReader<Map<String, Object>> reader = adapter.createReader(fileConfig);
        assertNotNull(reader);
        assertTrue(reader instanceof com.truist.batch.reader.JdbcRecordReader);
    }

    @Test
    void testValidateConfiguration() {
        // Valid configuration
        FileConfig validConfig = new FileConfig();
        validConfig.setTarget("TEST_TABLE");
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        params.put("fetchSize", "500");
        params.put("pageSize", "1000");
        params.put("sortKey", "ID");
        validConfig.setParams(params);
        
        // Should not throw exception
        assertDoesNotThrow(() -> adapter.validateConfiguration(validConfig));
    }

    @Test
    void testValidateConfigurationMissingTarget() {
        // Invalid configuration - missing target
        FileConfig invalidConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("JDBC adapter requires 'target'"));
    }

    @Test
    void testValidateConfigurationInvalidFetchSize() {
        // Invalid configuration - invalid fetchSize
        FileConfig invalidConfig = new FileConfig();
        invalidConfig.setTarget("TEST_TABLE");
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        params.put("fetchSize", "invalid");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("fetchSize must be a valid integer"));
    }

    @Test
    void testValidateConfigurationNegativeFetchSize() {
        // Invalid configuration - negative fetchSize
        FileConfig invalidConfig = new FileConfig();
        invalidConfig.setTarget("TEST_TABLE");
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        params.put("fetchSize", "-1");
        invalidConfig.setParams(params);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.validateConfiguration(invalidConfig)
        );
        
        assertTrue(exception.getMessage().contains("fetchSize must be positive"));
    }

    @Test
    void testGetPriority() {
        assertEquals(100, adapter.getPriority());
    }

    @Test
    void testGetAdapterName() {
        assertEquals("JdbcDataSourceAdapter", adapter.getAdapterName());
    }
}
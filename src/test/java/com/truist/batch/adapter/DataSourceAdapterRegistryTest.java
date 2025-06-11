package com.truist.batch.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truist.batch.model.FileConfig;

@ExtendWith(MockitoExtension.class)
class DataSourceAdapterRegistryTest {

    private TestJdbcAdapter jdbcAdapter;
    private TestRestAdapter restAdapter;
    private TestConflictAdapter conflictAdapter;
    private DataSourceAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        // Use real test implementations instead of mocks to avoid Mockito complexity
        jdbcAdapter = new TestJdbcAdapter();
        restAdapter = new TestRestAdapter();
        conflictAdapter = new TestConflictAdapter();
        
        // Debug: Test our adapters directly before creating registry
        System.out.println("=== DIRECT ADAPTER TESTING ===");
        System.out.println("TestJdbcAdapter supports 'jdbc': " + jdbcAdapter.supports("jdbc"));
        System.out.println("TestJdbcAdapter supports 'database': " + jdbcAdapter.supports("database"));
        System.out.println("TestJdbcAdapter supports 'sql': " + jdbcAdapter.supports("sql"));
        System.out.println("TestRestAdapter supports 'rest': " + restAdapter.supports("rest"));
        System.out.println("TestRestAdapter supports 'api': " + restAdapter.supports("api"));
        System.out.println("================================");
        
        // Create registry with test adapters
        registry = new DataSourceAdapterRegistry(List.of(jdbcAdapter, restAdapter, conflictAdapter));
        
        // Debug output
        System.out.println("Registry created. Supported formats: " + registry.getSupportedFormats());
    }

    @Test
    void testDirectAdapterBehavior() {
        // Test our test adapters directly to make sure they work as expected
        TestJdbcAdapter directJdbc = new TestJdbcAdapter();
        TestRestAdapter directRest = new TestRestAdapter();
        
        // JDBC Adapter tests
        assertTrue(directJdbc.supports("jdbc"));
        assertTrue(directJdbc.supports("database"));
        assertTrue(directJdbc.supports("sql"));
        assertFalse(directJdbc.supports("rest"));
        
        // REST Adapter tests  
        assertTrue(directRest.supports("rest"));
        assertTrue(directRest.supports("api"));
        assertFalse(directRest.supports("jdbc"));
        
        System.out.println("✅ Direct adapter behavior test passed");
    }

    @Test 
    void testGetAdapterByFormat() {
        // Debug: Print what formats are actually supported
        System.out.println("=== FINAL REGISTRY STATE ===");
        System.out.println("Supported formats: " + registry.getSupportedFormats());
        System.out.println("All adapters count: " + registry.getAllAdapters().size());
        
        // Verify our specific adapter is registered
        try {
            DataSourceAdapter foundAdapter = registry.getAdapter("jdbc");
            System.out.println("Found adapter for 'jdbc': " + foundAdapter.getAdapterName());
        } catch (Exception e) {
            System.out.println("ERROR finding adapter for 'jdbc': " + e.getMessage());
        }
        
        try {
            DataSourceAdapter foundAdapter = registry.getAdapter("database");
            System.out.println("Found adapter for 'database': " + foundAdapter.getAdapterName());
        } catch (Exception e) {
            System.out.println("ERROR finding adapter for 'database': " + e.getMessage());
        }
        System.out.println("=============================");
        
        // Test getting adapters by format
        assertEquals(jdbcAdapter, registry.getAdapter("jdbc"));
        assertEquals(jdbcAdapter, registry.getAdapter("database"));
        assertEquals(restAdapter, registry.getAdapter("rest")); // Higher priority wins
        assertEquals(restAdapter, registry.getAdapter("api"));
    }

    @Test
    void testGetAdapterCaseInsensitive() {
        // Test case insensitive format matching
        assertEquals(jdbcAdapter, registry.getAdapter("JDBC"));
        assertEquals(jdbcAdapter, registry.getAdapter("Database"));
        assertEquals(restAdapter, registry.getAdapter("REST"));
        assertEquals(restAdapter, registry.getAdapter("Api"));
    }

    @Test
    void testGetAdapterUnknownFormat() {
        // Test exception for unknown format
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.getAdapter("unknown")
        );
        
        assertTrue(exception.getMessage().contains("No adapter found for format 'unknown'"));
        assertTrue(exception.getMessage().contains("Supported formats:"));
    }

    @Test
    void testGetAdapterNullFormat() {
        // Test exception for null format
        assertThrows(IllegalArgumentException.class, () -> registry.getAdapter(null));
        assertThrows(IllegalArgumentException.class, () -> registry.getAdapter(""));
        assertThrows(IllegalArgumentException.class, () -> registry.getAdapter("   "));
    }

    @Test
    void testCreateReader() {
        // Setup file config
        FileConfig fileConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        fileConfig.setTarget("TEST_TABLE");
        fileConfig.setParams(params);
        
        // Test reader creation
        assertNotNull(registry.createReader(fileConfig));
    }

    @Test
    void testCreateReaderMissingFormat() {
        // Test exception when format is missing
        FileConfig fileConfig = new FileConfig();
        fileConfig.setParams(new HashMap<>());
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.createReader(fileConfig)
        );
        
        assertTrue(exception.getMessage().contains("FileConfig must specify 'format' parameter"));
    }

    @Test
    void testCreateReaderValidationFailure() {
        // Setup file config with invalid configuration
        FileConfig fileConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        // Missing required target - should cause validation failure
        fileConfig.setParams(params);
        
        // Test validation exception propagation
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.createReader(fileConfig)
        );
        
        assertTrue(exception.getMessage().contains("Invalid configuration for format 'jdbc'"));
    }

    @Test
    void testGetSupportedFormats() {
        // Test getting all supported formats
        var supportedFormats = registry.getSupportedFormats();
        
        assertTrue(supportedFormats.contains("jdbc"));
        assertTrue(supportedFormats.contains("database"));
        assertTrue(supportedFormats.contains("rest"));
        assertTrue(supportedFormats.contains("api"));
        assertTrue(supportedFormats.size() >= 4);
    }

    // Test implementations to avoid Mockito complexity
    private static class TestJdbcAdapter implements DataSourceAdapter {
        @Override
        public boolean supports(String format) {
            return "jdbc".equalsIgnoreCase(format) || 
                   "database".equalsIgnoreCase(format) || 
                   "sql".equalsIgnoreCase(format);
        }

        @Override
        public org.springframework.batch.item.ItemReader<Map<String, Object>> createReader(FileConfig fileConfig) {
            return mock(org.springframework.batch.item.ItemReader.class);
        }

        @Override
        public void validateConfiguration(FileConfig fileConfig) {
            if (fileConfig.getTarget() == null) {
                throw new IllegalArgumentException("Target required for JDBC");
            }
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public String getAdapterName() {
            return "TestJdbcAdapter";
        }
    }

    private static class TestRestAdapter implements DataSourceAdapter {
        @Override
        public boolean supports(String format) {
            return "rest".equalsIgnoreCase(format) || 
                   "api".equalsIgnoreCase(format) ||
                   "http".equalsIgnoreCase(format) ||
                   "https".equalsIgnoreCase(format);
        }

        @Override
        public org.springframework.batch.item.ItemReader<Map<String, Object>> createReader(FileConfig fileConfig) {
            return mock(org.springframework.batch.item.ItemReader.class);
        }

        @Override
        public int getPriority() {
            return 50;
        }

        @Override
        public String getAdapterName() {
            return "TestRestAdapter";
        }
    }

    private static class TestConflictAdapter implements DataSourceAdapter {
        @Override
        public boolean supports(String format) {
            return "rest".equalsIgnoreCase(format);
        }

        @Override
        public org.springframework.batch.item.ItemReader<Map<String, Object>> createReader(FileConfig fileConfig) {
            return mock(org.springframework.batch.item.ItemReader.class);
        }

        @Override
        public int getPriority() {
            return 25; // Lower than TestRestAdapter
        }

        @Override
        public String getAdapterName() {
            return "TestConflictAdapter";
        }
    }
}
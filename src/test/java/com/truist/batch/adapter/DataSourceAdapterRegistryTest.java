package com.truist.batch.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truist.batch.model.FileConfig;

@ExtendWith(MockitoExtension.class)
class DataSourceAdapterRegistryTest {

    @Mock
    private DataSourceAdapter jdbcAdapter;
    
    @Mock
    private DataSourceAdapter restAdapter;
    
    @Mock
    private DataSourceAdapter conflictAdapter;
    
    private DataSourceAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        // Setup mock adapters
        when(jdbcAdapter.supports("jdbc")).thenReturn(true);
        when(jdbcAdapter.supports("database")).thenReturn(true);
        when(jdbcAdapter.getAdapterName()).thenReturn("JdbcAdapter");
        when(jdbcAdapter.getPriority()).thenReturn(100);
        
        when(restAdapter.supports("rest")).thenReturn(true);
        when(restAdapter.supports("api")).thenReturn(true);
        when(restAdapter.getAdapterName()).thenReturn("RestAdapter");
        when(restAdapter.getPriority()).thenReturn(50);
        
        when(conflictAdapter.supports("rest")).thenReturn(true);
        when(conflictAdapter.getAdapterName()).thenReturn("ConflictAdapter");
        when(conflictAdapter.getPriority()).thenReturn(25); // Lower priority than restAdapter
        
        // Create registry with mock adapters
        registry = new DataSourceAdapterRegistry(List.of(jdbcAdapter, restAdapter, conflictAdapter));
    }

    @Test
    void testAdapterDiscovery() {
        // Verify all adapters are discovered
        assertEquals(3, registry.getAllAdapters().size());
        assertTrue(registry.getAllAdapters().contains(jdbcAdapter));
        assertTrue(registry.getAllAdapters().contains(restAdapter));
        assertTrue(registry.getAllAdapters().contains(conflictAdapter));
    }

    @Test
    void testGetAdapterByFormat() {
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
        fileConfig.setParams(params);
        
        // Mock adapter behavior
        when(jdbcAdapter.createReader(fileConfig)).thenReturn(mock(org.springframework.batch.item.ItemReader.class));
        
        // Test reader creation
        assertNotNull(registry.createReader(fileConfig));
        verify(jdbcAdapter).validateConfiguration(fileConfig);
        verify(jdbcAdapter).createReader(fileConfig);
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
        // Setup file config
        FileConfig fileConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        fileConfig.setParams(params);
        
        // Mock validation failure
        doThrow(new IllegalArgumentException("Invalid config"))
            .when(jdbcAdapter).validateConfiguration(fileConfig);
        
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
}
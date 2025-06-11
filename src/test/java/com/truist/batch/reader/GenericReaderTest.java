package com.truist.batch.reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import com.truist.batch.adapter.DataSourceAdapterRegistry;
import com.truist.batch.model.FileConfig;

@ExtendWith(MockitoExtension.class)
class GenericReaderTest {

    @Mock
    private DataSourceAdapterRegistry adapterRegistry;
    
    @Mock
    private ItemStreamReader<Map<String, Object>> mockReader;
    
    private FileConfig fileConfig;
    private GenericReader genericReader;

    @BeforeEach
    void setUp() {
        fileConfig = new FileConfig();
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        fileConfig.setParams(params);
        fileConfig.setTarget("TEST_TABLE");
        
        genericReader = new GenericReader(fileConfig, adapterRegistry);
    }

    @Test
    void testOpenWithItemStreamReader() throws Exception {
        // Setup mocks
        when(adapterRegistry.createReader(fileConfig)).thenReturn(mockReader);
        
        ExecutionContext context = new ExecutionContext();
        
        // Test open
        genericReader.open(context);
        
        // Verify adapter registry was called
        verify(adapterRegistry).createReader(fileConfig);
        verify(mockReader).open(context);
    }

    @Test
    void testRead() throws Exception {
        // Setup mocks
        when(adapterRegistry.createReader(fileConfig)).thenReturn(mockReader);
        Map<String, Object> expectedRecord = Map.of("id", 1, "name", "test");
        when(mockReader.read()).thenReturn(expectedRecord);
        
        // Open and read
        genericReader.open(new ExecutionContext());
        Map<String, Object> result = genericReader.read();
        
        // Verify
        assertEquals(expectedRecord, result);
        verify(mockReader).read();
    }

    @Test
    void testUpdate() throws Exception {
        // Setup mocks
        when(adapterRegistry.createReader(fileConfig)).thenReturn(mockReader);
        
        ExecutionContext context = new ExecutionContext();
        genericReader.open(context);
        
        // Test update
        genericReader.update(context);
        
        // Verify
        verify(mockReader).update(context);
    }

    @Test
    void testClose() throws Exception {
        // Setup mocks
        when(adapterRegistry.createReader(fileConfig)).thenReturn(mockReader);
        
        genericReader.open(new ExecutionContext());
        
        // Test close
        genericReader.close();
        
        // Verify
        verify(mockReader).close();
    }

    @Test
    void testOpenFailure() throws Exception {
        // Setup mock to throw exception
        when(adapterRegistry.createReader(fileConfig))
            .thenThrow(new IllegalArgumentException("Invalid configuration"));
        
        // Test exception handling
        assertThrows(
            org.springframework.batch.item.ItemStreamException.class,
            () -> genericReader.open(new ExecutionContext())
        );
    }
}
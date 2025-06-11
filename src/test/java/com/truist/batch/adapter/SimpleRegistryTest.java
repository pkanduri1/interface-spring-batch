package com.truist.batch.adapter;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;

public class SimpleRegistryTest {

    @Test
    void testBasicRegistryCreation() {
        System.out.println("=== TESTING BASIC REGISTRY ===");
        
        // Create a simple test adapter
        DataSourceAdapter simpleAdapter = new DataSourceAdapter() {
            @Override
            public boolean supports(String format) {
                System.out.println("SimpleAdapter.supports(" + format + ") called");
                return "test".equals(format) || "database".equals(format);
            }
            
            @Override
            public org.springframework.batch.item.ItemReader<java.util.Map<String, Object>> createReader(com.truist.batch.model.FileConfig fileConfig) {
                return null;
            }
            
            @Override
            public String getAdapterName() {
                return "SimpleTestAdapter";
            }
            
            @Override
            public int getPriority() {
                return 100;
            }
        };
        
        System.out.println("Creating registry with simple adapter...");
        
        // Create registry - this should trigger our debug output
        List<DataSourceAdapter> adapters = new ArrayList<>();
        adapters.add(simpleAdapter);
        DataSourceAdapterRegistry registry = new DataSourceAdapterRegistry(adapters);
        
        System.out.println("Registry created. Supported formats: " + registry.getSupportedFormats());
        System.out.println("================================");
    }
}
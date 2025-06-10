package com.truist.batch.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;

/**
 * Integration test to verify that the real p327.yml mapping file
 * loads correctly via YamlMappingService.
 */
@SpringBootTest
public class YamlMappingServiceIntegrationTest {

    @Autowired
    private YamlMappingService mappingService;

    private static final String TEMPLATE = "p327/hr/p327.yml";  // ✅ FIX: Use actual file path

    @Test
    void testLoadDefaultMapping() {
        // ✅ FIX: Use actual existing YAML file path
        List<YamlMapping> mappings = mappingService.loadYamlMappings(TEMPLATE);
        assertFalse(mappings.isEmpty(), "Mapping list should not be empty");

        // Find the default mapping
        YamlMapping defaultMapping = mappings.stream()
                .filter(m -> "default".equalsIgnoreCase(m.getTransactionType()))
                .findFirst()
                .orElse(null);
        assertNotNull(defaultMapping, "Default transactionType mapping must exist");

        Map<String, FieldMapping> fields = defaultMapping.getFields();
        assertFalse(fields.isEmpty(), "Default mapping must contain at least one field");

        // Verify a known field appears and its properties
        FieldMapping locCode = fields.get("location-code");
        assertNotNull(locCode, "location-code mapping must be present");
        assertEquals("location-code", locCode.getTargetField());
        assertEquals(1, locCode.getTargetPosition());
        assertEquals(6, locCode.getLength());
    }

    @Test
    void testTransformFieldForSampleRow() {
        // Prepare a sample raw row map
        Map<String,Object> row = Map.of(
            "location_code", "ABC123",
            "acct_num",    "000012345"
        );
        
        // Get mapping and transform
        YamlMapping mapping = mappingService.getMapping(TEMPLATE, "default");
        List<FieldMapping> ordered = mapping.getFields().entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted((a,b) -> Integer.compare(a.getTargetPosition(), b.getTargetPosition()))
                .collect(Collectors.toList());
        
        // Test that we can get the first field mapping
        assertNotNull(ordered.get(0), "Should have at least one field mapping");
        
        // Test transformation (this will use your existing transformField logic)
        String transformedLocation = mappingService.transformField(row, ordered.get(0));
        assertNotNull(transformedLocation, "Transformed value should not be null");
    }
}
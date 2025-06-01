package com.truist.batch.processor;

import org.junit.jupiter.api.Test;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FileConfig;
import com.truist.batch.processor.GenericProcessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.LinkedHashMap;

// Assume FileConfig, GenericProcessor, and mappingService are available in the test context.

public class GenericProcessorTest {

    // Existing test method assumed here...
	// Mock the mappingService
    YamlMappingService mappingService = mock(YamlMappingService.class);

    @Test
    public void testGenericProcessor_constantDefaults() throws Exception {
        // FileConfig remains pointing to 'p327'
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTemplate("p327");
        fileConfig.setParams(Map.of(
            "batchDateParam", "batch_date",
            "batchDateValue", "20250101"
        ));
     // Mock the mappingService
        YamlMappingService mappingService = mock(YamlMappingService.class);
        // Provide only ACCT_NUM and TOTAL-DELINQ-AMT to focus on constants
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("ACCT_NUM", "000098765");
        row.put("TOTAL-DELINQ-AMT", "123456");

        GenericProcessor processor = new GenericProcessor(fileConfig, mappingService);
        Map<String,Object> output = processor.process(row);

        // LOCATION-CODE should use constant default "100020"
        String locCode = (String) output.get("LOCATION-CODE");
        assertEquals("100020", locCode.trim(), "LOCATION-CODE default constant");

        // CREDIT-LIMIT-AMT default is "0"
        String creditLimit = (String) output.get("CREDIT-LIMIT-AMT");
        assertTrue(creditLimit.trim().equals("0"), "CREDIT-LIMIT-AMT default constant");

        // TOTAL-DELINQ-AMT uses source field value
        String totalDelinq = (String) output.get("TOTAL-DELINQ-AMT");
        assertEquals("123456", totalDelinq.trim(), "TOTAL-DELINQ-AMT source field");
    }

    @Test
    public void testGenericProcessor_fieldLengths() throws Exception {
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTemplate("p327");
        fileConfig.setParams(Map.of(
            "batchDateParam", "batch_date",
            "batchDateValue", "20250101"
        ));

        // Minimal row with ACCT_NUM only
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("ACCT_NUM", "ABC");

        GenericProcessor processor = new GenericProcessor(fileConfig, mappingService);
        Map<String,Object> output = processor.process(row);

        // Verify each output value matches its mapping length
        var yamlMapping = mappingService.getMapping("p327", null);
        yamlMapping.getFields().values().stream()
            .forEach(m -> {
                String val = (String) output.get(m.getTargetField());
                assertNotNull(val, "Value should not be null for " + m.getTargetField());
                assertEquals(m.getLength(), val.length(),
                    "Field " + m.getTargetField() + " should be length " + m.getLength());
            });
    }
}

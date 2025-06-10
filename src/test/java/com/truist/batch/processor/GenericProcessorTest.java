package com.truist.batch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.YamlMapping;

// Assume FileConfig, GenericProcessor, and mappingService are available in the test context.

@ExtendWith(MockitoExtension.class)
public class GenericProcessorTest {

    @Mock
    private YamlMappingService mappingService;

    @Test
    public void testGenericProcessor_constantDefaults() throws Exception {
        // ✅ FIX: Properly setup FileConfig and mock
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTemplate("p327");
        fileConfig.setTransactionType("default");
        fileConfig.setParams(Map.of(
            "batchDateParam", "batch_date",
            "batchDateValue", "20250101"
        ));

        // ✅ FIX: Create proper YamlMapping with fields
        YamlMapping yamlMapping = new YamlMapping();
        yamlMapping.setFileType("p327");
        yamlMapping.setTransactionType("default");
        
        // Create field mappings
        Map<String, FieldMapping> fields = new LinkedHashMap<>();
        
        // LOCATION-CODE field
        FieldMapping locationCodeField = new FieldMapping();
        locationCodeField.setFieldName("location-code");
        locationCodeField.setTargetField("LOCATION-CODE");
        locationCodeField.setTargetPosition(1);
        locationCodeField.setLength(6);
        locationCodeField.setTransformationType("constant");
        locationCodeField.setDefaultValue("100020");
        fields.put("location-code", locationCodeField);
        
        // CREDIT-LIMIT-AMT field
        FieldMapping creditLimitField = new FieldMapping();
        creditLimitField.setFieldName("credit-limit-amt");
        creditLimitField.setTargetField("CREDIT-LIMIT-AMT");
        creditLimitField.setTargetPosition(3);
        creditLimitField.setLength(13);
        creditLimitField.setTransformationType("constant");
        creditLimitField.setDefaultValue("0");
        fields.put("credit-limit-amt", creditLimitField);
        
        // TOTAL-DELINQ-AMT field
        FieldMapping totalDelinqField = new FieldMapping();
        totalDelinqField.setFieldName("total-delinq-amt");
        totalDelinqField.setTargetField("TOTAL-DELINQ-AMT");
        totalDelinqField.setTargetPosition(16);
        totalDelinqField.setLength(19);
        totalDelinqField.setTransformationType("source");
        totalDelinqField.setSourceField("TOTAL-DELINQ-AMT");
        fields.put("total-delinq-amt", totalDelinqField);
        
        yamlMapping.setFields(fields);
        
        // ✅ FIX: Mock the getMapping call
        when(mappingService.getMapping("p327", "default")).thenReturn(yamlMapping);
        
        // ✅ FIX: Mock the transformField calls
        when(mappingService.transformField(any(), eq(locationCodeField))).thenReturn("100020");
        when(mappingService.transformField(any(), eq(creditLimitField))).thenReturn("0000000000000");
        when(mappingService.transformField(any(), eq(totalDelinqField))).thenReturn("0000000000123456000");

        // Test data
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ACCT_NUM", "000098765");
        row.put("TOTAL-DELINQ-AMT", "123456");

        GenericProcessor processor = new GenericProcessor(fileConfig, mappingService);
        Map<String, Object> output = processor.process(row);

        // Verify results
        assertEquals("100020", output.get("LOCATION-CODE"));
        assertEquals("0000000000000", output.get("CREDIT-LIMIT-AMT"));
        assertEquals("0000000000123456000", output.get("TOTAL-DELINQ-AMT"));
    }

    @Test
    public void testGenericProcessor_fieldLengths() throws Exception {
        // ✅ FIX: Simplified test with proper mocking
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTemplate("p327");
        fileConfig.setTransactionType("default");
        fileConfig.setParams(Map.of(
            "batchDateParam", "batch_date",
            "batchDateValue", "20250101"
        ));

        // Create simple mapping with one field
        YamlMapping yamlMapping = new YamlMapping();
        yamlMapping.setFileType("p327");
        yamlMapping.setTransactionType("default");
        
        Map<String, FieldMapping> fields = new LinkedHashMap<>();
        FieldMapping testField = new FieldMapping();
        testField.setFieldName("test-field");
        testField.setTargetField("TEST-FIELD");
        testField.setTargetPosition(1);
        testField.setLength(10);
        testField.setTransformationType("constant");
        testField.setDefaultValue("TEST");
        fields.put("test-field", testField);
        
        yamlMapping.setFields(fields);
        
        // Mock the calls
        when(mappingService.getMapping("p327", "default")).thenReturn(yamlMapping);
        when(mappingService.transformField(any(), eq(testField))).thenReturn("TEST      "); // 10 chars

        // Test data
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ACCT_NUM", "ABC");

        GenericProcessor processor = new GenericProcessor(fileConfig, mappingService);
        Map<String, Object> output = processor.process(row);

        // Verify the field exists and has correct length
        String testValue = (String) output.get("TEST-FIELD");
        assertNotNull(testValue);
        assertEquals(10, testValue.length());
    }
}
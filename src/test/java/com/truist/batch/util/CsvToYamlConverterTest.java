package com.truist.batch.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.Condition; 
import com.truist.batch.model.FieldMapping; 
import com.truist.batch.model.YamlMapping; 

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvToYamlConverterTest {

    @TempDir
    Path tempDir;

    private Path createTestCsv(String fileName, String... lines) throws IOException {
        Path csvPath = tempDir.resolve(fileName);
        try (FileWriter writer = new FileWriter(csvPath.toFile())) {
            for (String line : lines) {
                writer.write(line + System.lineSeparator());
            }
        }
        return csvPath;
    }

    @Test
    @SuppressWarnings("unchecked")
    void testConvert_HappyPath_GeneratesCorrectYaml() throws Exception {
        String header = "Field Name,Target Position,Length,Data Type,Format,Pad,Transformation Type,Transform,Default Value,Target Field Name,Source Field Name,If Expression,Else If Expression,Else";
        // Corrected row1: Added missing comma, ensured correct Java string escaping for quotes
        String row1 = "LOCATION_CODE,1,6,String,XYZ_FORMAT,right,constant,UPPER,100030,TARGET_LOC,SOURCE_LOC,SourceX == 'Trim',\"Source1 == \\\"B\\\"|Source1 == \\\"C\\\"\",SourceX == 'Other'";
        Path csvPath = createTestCsv("happy_path.csv", header, row1);
        Path yamlOutputPath = tempDir.resolve("output.yaml");

        CsvToYamlConverter.convert(csvPath.toString(), yamlOutputPath.toString(), "P327_TEST");

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> actualYamlMap = yamlMapper.readValue(yamlOutputPath.toFile(), new TypeReference<Map<String, Object>>() {});

        assertNotNull(actualYamlMap, "Generated YAML map should not be null");
        assertEquals("P327_TEST", actualYamlMap.get("fileType"), "File type should match");

        assertTrue(actualYamlMap.containsKey("fields"), "YAML should contain 'fields' map");
        Map<String, Map<String, Object>> fields = (Map<String, Map<String, Object>>) actualYamlMap.get("fields");
        assertNotNull(fields, "'fields' map should not be null");
        assertTrue(fields.containsKey("LOCATION_CODE"), "Fields should contain 'LOCATION_CODE'");

        Map<String, Object> fieldData = fields.get("LOCATION_CODE");
        assertNotNull(fieldData, "'LOCATION_CODE' field data should not be null");

        assertEquals("LOCATION_CODE", fieldData.get("fieldName"));
        assertEquals(1, (Integer) fieldData.get("targetPosition"));
        assertEquals(6, (Integer) fieldData.get("length"));
        assertEquals("String", fieldData.get("dataType"));
        assertEquals("XYZ_FORMAT", fieldData.get("format"));
        assertEquals("right", fieldData.get("pad"));
        assertEquals("constant", fieldData.get("transformationType"));
        assertEquals("UPPER", fieldData.get("transform")); 
        assertEquals("100030", fieldData.get("defaultValue")); // Corrected from "value" to "defaultValue"
        assertEquals("TARGET_LOC", fieldData.get("targetField"));
        assertEquals("SOURCE_LOC", fieldData.get("sourceField"));
        assertEquals(" ", fieldData.get("padChar")); 

        assertTrue(fieldData.containsKey("conditions"), "Field should have conditions list");
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) fieldData.get("conditions");
        assertNotNull(conditions);
        assertEquals(1, conditions.size(), "Should have one main condition object");

        Map<String, Object> mainCondition = conditions.get(0);
        assertEquals("SourceX == 'Trim'", mainCondition.get("ifExpr"));
        assertEquals("SourceX == 'Other'", mainCondition.get("elseExpr"));

        assertTrue(mainCondition.containsKey("elseIfExprs"), "Main condition should have 'elseIfExprs' list");
        List<Map<String, Object>> elseIfConditions = (List<Map<String, Object>>) mainCondition.get("elseIfExprs");
        assertNotNull(elseIfConditions);
        assertEquals(2, elseIfConditions.size(), "Should have two 'else if' conditions");
        
        // These assertions are based on the prompt's code, which might be incorrect based on previous test failures
        assertEquals("Source1 == \"B\"", elseIfConditions.get(0).get("ifExpr"));
        assertEquals("Source1 == \"C\"", elseIfConditions.get(1).get("ifExpr"));
    }
}

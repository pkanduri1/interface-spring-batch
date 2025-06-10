package com.truist.batch.writer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.YamlMapping;
import com.truist.batch.writer.GenericWriter;

class GenericWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void testGenericWriterWritesFixedWidthRecord() throws Exception {
        // Prepare a temporary output file
        Path outputFile = tempDir.resolve("output.dat");

        // Configure FileConfig with template and outputPath
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTemplate("testTemplate");
        fileConfig.setTransactionType("default");
        Map<String, String> params = new HashMap<>();
        params.put("outputPath", outputFile.toString());
        fileConfig.setParams(params);

        // Mock the mappingService
        YamlMappingService mappingService = mock(YamlMappingService.class);

        // ✅ FIX: Create proper YamlMapping with fields
        YamlMapping yamlMapping = new YamlMapping();
        yamlMapping.setFileType("testTemplate");
        yamlMapping.setTransactionType("default");
        
        // Create field mappings
        Map<String, FieldMapping> fields = new LinkedHashMap<>();
        
        FieldMapping f1 = new FieldMapping();
        f1.setTargetField("F1");
        f1.setTargetPosition(1);
        f1.setLength(2);
        fields.put("f1", f1);
        
        FieldMapping f2 = new FieldMapping();
        f2.setTargetField("F2");
        f2.setTargetPosition(2);
        f2.setLength(2);
        fields.put("f2", f2);
        
        yamlMapping.setFields(fields);

        // ✅ FIX: Mock the getMapping call to return our YamlMapping
        when(mappingService.getMapping("testTemplate", "default")).thenReturn(yamlMapping);

        // Instantiate and open writer
        GenericWriter writer = new GenericWriter(mappingService, fileConfig);
        ExecutionContext context = new ExecutionContext();
        writer.open(context);

        // Write a single dummy row with transformed values
        Map<String, Object> row = new HashMap<>();
        row.put("F1", "X1");  // Already transformed values
        row.put("F2", "Y2");  // Already transformed values
        
        writer.write(new Chunk<>(List.of(row)));
        writer.close();

        // Read and verify output
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size(), "Should write one line");
        assertEquals("X1Y2", lines.get(0), "Line should be concatenation of transformed fields");
    }
}

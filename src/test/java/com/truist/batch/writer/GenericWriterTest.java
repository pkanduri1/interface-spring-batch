package com.truist.batch.writer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FileConfig;
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
        Map<String, String> params = new HashMap<>();
        params.put("outputPath", outputFile.toString());
        fileConfig.setParams(params);

        // Mock the mappingService
        YamlMappingService mappingService = mock(YamlMappingService.class);

        // Create two FieldMappings
        FieldMapping m1 = new FieldMapping();
        m1.setTargetField("F1");
        FieldMapping m2 = new FieldMapping();
        m2.setTargetField("F2");

        // Return mappings in specific order
        List<Entry<String, FieldMapping>> mappings = List.of(
            new AbstractMap.SimpleEntry<>("e1", m1),
            new AbstractMap.SimpleEntry<>("e2", m2)
        );
        when(mappingService.loadFieldMappings("testTemplate")).thenReturn(mappings);

        // Stub transformField to return fixed values
        when(mappingService.transformField(anyMap(), eq(m1))).thenReturn("X1");
        when(mappingService.transformField(anyMap(), eq(m2))).thenReturn("Y2");

        // Instantiate and open writer
        GenericWriter writer = new GenericWriter(mappingService, fileConfig);
        ExecutionContext context = new ExecutionContext();
        writer.open(context);

        // Write a single dummy row
        Map<String, Object> row = new HashMap<>();
        writer.write(new Chunk<>(List.of(row)));
        writer.close();

        // Read and verify output
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size(), "Should write one line");
        assertEquals("X1Y2", lines.get(0), "Line should be concatenation of transformed fields");
    }
}

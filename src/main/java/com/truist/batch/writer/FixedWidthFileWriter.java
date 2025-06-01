package com.truist.batch.writer;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Writes records to a fixed-width file based on mapping definitions.
 * Enhanced with proper directory creation and error handling.
 */
@Slf4j
@RequiredArgsConstructor
public class FixedWidthFileWriter implements ItemWriter<String>, ItemStream {

    private final YamlMappingService mappingService;
    private final String template;      // mapping YAML path
    private final String outputPath;    // file system path to write
    private FlatFileItemWriter<String> delegate;
    private List<Map.Entry<String, FieldMapping>> mappings;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            // Ensure output directory exists
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created && !parentDir.exists()) {
                    throw new ItemStreamException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
                log.debug("📂 Created directory: {}", parentDir.getAbsolutePath());
            }
            
            // Load and sort mappings once per job execution
            mappings = mappingService.loadFieldMappings(template);
            log.debug("📋 Loaded {} mappings from template: {}", mappings.size(), template);
            
            // Initialize delegate writer for String lines
            delegate = new FlatFileItemWriter<>();
            delegate.setResource(new FileSystemResource(outputPath));
            delegate.setName("FixedWidthFileWriter");
            delegate.setAppendAllowed(false);
            
            // Since mappingService.transformField already pads values, identity aggregator suffices
            delegate.setLineAggregator(line -> line);
            delegate.open(executionContext);
            
            log.info("✅ Opened file writer for: {}", outputPath);
            
        } catch (Exception e) {
            log.error("❌ Failed to open FixedWidthFileWriter for: {}", outputPath, e);
            throw new ItemStreamException("Failed to initialize file writer", e);
        }
    }

    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        // Write pre-formatted fixed-width strings directly
        delegate.write(chunk);
        
        log.debug("📝 Wrote {} records to: {}", chunk.size(), outputPath);
    }

    /**
     * Maps a record to a fixed-width string using the loaded mappings.
     * This method is available for direct use if needed.
     */
    public String mapRecord(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, FieldMapping> entry : mappings) {
            FieldMapping m = entry.getValue();
            String val = mappingService.transformField(row, m);
            sb.append(val);
        }
        return sb.toString();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (delegate != null) {
            delegate.update(executionContext);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (delegate != null) {
            delegate.close();
            log.info("🔒 Closed file writer for: {}", outputPath);
        }
    }
}
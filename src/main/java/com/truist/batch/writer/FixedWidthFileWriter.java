package com.truist.batch.writer;

import java.io.File;
import java.util.Map;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import com.truist.batch.mapping.YamlMappingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes pre-formatted fixed-width strings to a file.
 * GenericWriter handles the mapping transformation - this just writes the strings.
 */
@Slf4j
@RequiredArgsConstructor
public class FixedWidthFileWriter implements ItemWriter<String>, ItemStream {

    private final YamlMappingService mappingService;
    private final String template;      // kept for potential mapRecord usage
    private final String outputPath;
    private FlatFileItemWriter<String> delegate;

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
            
            // ✅ REMOVED: No longer load mappings here since GenericWriter handles transformation
            // GenericWriter passes pre-formatted strings to this writer
            
            // Initialize delegate writer for String lines
            delegate = new FlatFileItemWriter<>();
            delegate.setResource(new FileSystemResource(outputPath));
            delegate.setName("FixedWidthFileWriter");
            delegate.setAppendAllowed(false);
            
            // Identity aggregator since strings are already formatted
            delegate.setLineAggregator(line -> line);
         // Add before delegate.open()
           // Thread.sleep(100 * (outputPath.hashCode() % 10)); // Stagger file creation
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
     * Legacy method - kept for backward compatibility but not used in normal flow.
     * GenericWriter handles the transformation instead.
     */
    @Deprecated
    public String mapRecord(Map<String, Object> row) {
        throw new UnsupportedOperationException("Use GenericWriter for mapping - this writer only handles pre-formatted strings");
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
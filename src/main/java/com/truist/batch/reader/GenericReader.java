package com.truist.batch.reader;

import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import com.truist.batch.adapter.DataSourceAdapterRegistry;
import com.truist.batch.model.FileConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced GenericReader that uses the DataSourceAdapter plugin architecture.
 * 
 * This reader delegates to the appropriate adapter based on the FileConfig.params.format,
 * enabling support for multiple source types through a clean plugin interface.
 * 
 * Supported formats (via adapters):
 * - jdbc: Database tables and custom SQL
 * - rest/api: REST API endpoints
 * - csv: Comma-separated files (via existing DelimitedOrFixedWidthReader)
 * - excel: Excel spreadsheets (via existing ExcelFileReader)
 * - And more via the plugin architecture!
 */
@Slf4j
public class GenericReader implements ItemStreamReader<Map<String, Object>>, ItemStream {
    
    private final FileConfig fileConfig;
    private final DataSourceAdapterRegistry adapterRegistry;
    private ItemStreamReader<Map<String, Object>> delegate;
    
    public GenericReader(FileConfig fileConfig, DataSourceAdapterRegistry adapterRegistry) {
        this.fileConfig = fileConfig;
        this.adapterRegistry = adapterRegistry;
    }
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        String format = fileConfig.getParams().get("format");
        
        log.info("🔌 GenericReader opening with format: '{}' for target: '{}'", 
            format, fileConfig.getTarget());
        
        try {
            // Use adapter registry to create appropriate reader
            ItemReader<Map<String, Object>> reader = adapterRegistry.createReader(fileConfig);
            
            // Ensure the reader supports ItemStream interface
            if (reader instanceof ItemStreamReader) {
                this.delegate = (ItemStreamReader<Map<String, Object>>) reader;
            } else {
                // Wrap non-stream readers in a simple wrapper
                this.delegate = new ItemStreamReaderWrapper(reader);
            }
            
            // Open the delegate reader
            this.delegate.open(executionContext);
            
            log.info("✅ GenericReader successfully opened using adapter for format: '{}'", format);
            
        } catch (Exception e) {
            log.error("❌ Failed to open GenericReader for format: '{}', target: '{}'", 
                format, fileConfig.getTarget(), e);
            throw new ItemStreamException("Failed to initialize GenericReader", e);
        }
    }
    
    @Override
    public Map<String, Object> read() throws Exception {
        return delegate.read();
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
            log.info("🔒 GenericReader closed successfully");
        }
    }
    
    /**
     * Simple wrapper to make non-ItemStream readers compatible with ItemStream interface.
     */
    private static class ItemStreamReaderWrapper implements ItemStreamReader<Map<String, Object>> {
        private final ItemReader<Map<String, Object>> delegate;
        
        public ItemStreamReaderWrapper(ItemReader<Map<String, Object>> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Map<String, Object> read() throws Exception {
            return delegate.read();
        }
        
        @Override
        public void open(ExecutionContext executionContext) throws ItemStreamException {
            // No-op for non-stream readers
        }
        
        @Override
        public void update(ExecutionContext executionContext) throws ItemStreamException {
            // No-op for non-stream readers
        }
        
        @Override
        public void close() throws ItemStreamException {
            // No-op for non-stream readers
        }
    }
}
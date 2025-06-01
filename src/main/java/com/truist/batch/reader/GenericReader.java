package com.truist.batch.reader;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import com.truist.batch.model.FileConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GenericReader delegates to the appropriate reader based on FileConfig.params or extension.
 */
@Slf4j
@RequiredArgsConstructor
public class GenericReader implements ItemStreamReader<Map<String, Object>>, ItemStream {
    private final FileConfig fileConfig;
    private final DataSource dataSource;
    private ItemStreamReader<Map<String, Object>> delegate;

    @Override
    public Map<String, Object> read() throws Exception {
        return delegate.read();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // Initialize delegate based on fileConfig.params.get("format") or inputPath extension
        String format = fileConfig.getParams().getOrDefault("format", "csv").toLowerCase();
        switch (format) {
            case "csv":
            case "pipe":
            case "fixed":
                delegate = new DelimitedOrFixedWidthReader(fileConfig);
                break;
            case "excel":
                delegate = new ExcelFileReader(fileConfig);
                break;
            case "jdbc":
                // Read SQL query from YAML params for complex joins/filters
                String sql = fileConfig.getParams().get("query");
                delegate = new JdbcRecordReader(fileConfig, dataSource, sql);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).update(executionContext);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).close();
        }
    }
}
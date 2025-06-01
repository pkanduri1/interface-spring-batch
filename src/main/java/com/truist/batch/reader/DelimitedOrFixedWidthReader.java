package com.truist.batch.reader;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.core.io.FileSystemResource;

import com.truist.batch.model.FileConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads delimited or fixed-width files into a Map<String,Object> per record.
 */
@Slf4j
@RequiredArgsConstructor
public class DelimitedOrFixedWidthReader implements ItemStreamReader<Map<String, Object>>, ItemStream {

    private final FileConfig fileConfig;
    private FlatFileItemReader<Map<String, Object>> delegate;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate = new FlatFileItemReader<>();
        delegate.setResource(new FileSystemResource(fileConfig.getInputPath()));

        DefaultLineMapper<Map<String, Object>> lineMapper = new DefaultLineMapper<>();
        String format = fileConfig.getParams().getOrDefault("format", "delimited").toLowerCase();

        if ("fixed".equals(format)) {
            FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
            // Example: columnRanges = "1-10,11-20"; columnNames = "field1,field2"
            String[] ranges = fileConfig.getParams().get("columnRanges").split(",");
            Range[] columns = new Range[ranges.length];
            for (int i = 0; i < ranges.length; i++) {
                String[] parts = ranges[i].split("-");
                columns[i] = new Range(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            tokenizer.setColumns(columns);
            String[] names = fileConfig.getParams().get("columnNames").split(",");
            tokenizer.setNames(names);
            lineMapper.setLineTokenizer(tokenizer);
        } else {
            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
            tokenizer.setDelimiter(fileConfig.getParams().getOrDefault("delimiter", ","));
            String[] names = fileConfig.getParams().get("columnNames").split(",");
            tokenizer.setNames(names);
            lineMapper.setLineTokenizer(tokenizer);
        }

        // Map FieldSet to Map<String,Object>
        lineMapper.setFieldSetMapper(fieldSet -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String name : fieldSet.getNames()) {
                map.put(name, fieldSet.readString(name));
            }
            return map;
        });

        delegate.setLineMapper(lineMapper);
        delegate.open(executionContext);
    }

    @Override
    public Map<String, Object> read() throws Exception {
        return delegate.read();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}

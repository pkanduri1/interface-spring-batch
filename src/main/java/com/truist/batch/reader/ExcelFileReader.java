package com.truist.batch.reader;

import com.truist.batch.model.FileConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.ExecutionContext;

import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.ItemStreamException;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Reads Excel (.xls/.xlsx) files into a Map<String,Object> per row.
 */
@Slf4j
public class ExcelFileReader extends AbstractItemStreamItemReader<Map<String, Object>> {

    private final FileConfig fileConfig;
    private Workbook workbook;
    private Iterator<Row> rowIterator;
    private List<String> headers;

    public ExcelFileReader(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
        setName("ExcelFileReader");
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            InputStream is = new FileInputStream(fileConfig.getInputPath());
            workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(cell.getStringCellValue());
                }
            }
        } catch (Exception e) {
            throw new ItemStreamException("Failed opening Excel file: " + fileConfig.getInputPath(), e);
        }
    }

    @Override
    public Map<String, Object> read() throws Exception {
        if (rowIterator == null || !rowIterator.hasNext()) {
            return null;
        }
        Row row = rowIterator.next();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Object value;
            switch (cell.getCellType()) {
                case NUMERIC: value = cell.getNumericCellValue(); break;
                case BOOLEAN: value = cell.getBooleanCellValue(); break;
                case STRING: value = cell.getStringCellValue(); break;
                default: value = cell.toString();
            }
            map.put(headers.get(i), value);
        }
        return map;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // No-op
    }

    @Override
    public void close() throws ItemStreamException {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (Exception ignored) {}
        }
    }
}

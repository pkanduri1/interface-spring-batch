package com.truist.batch.reader;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.truist.batch.model.FileConfig;
import com.truist.batch.reader.JdbcRecordReader;

public class JdbcRecordReaderIntegrationTest {

    private DataSource dataSource;

    @BeforeEach
    void setUpDatabase() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=Oracle");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;

        JdbcTemplate jdbc = new JdbcTemplate(ds);
        // Create a staging table
        jdbc.execute("""
            CREATE TABLE STG_TEST (
              ACCT_NUM VARCHAR(20),
              BATCH_DATE VARCHAR(8),
              TRANSACTION_TYPE VARCHAR(10)
            );
        """);
        // Insert sample rows
        jdbc.update(
            "INSERT INTO STG_TEST (ACCT_NUM, BATCH_DATE, TRANSACTION_TYPE) VALUES (?,?,?)",
            "12345", "20250101", "DEFAULT"
        );
        jdbc.update(
            "INSERT INTO STG_TEST (ACCT_NUM, BATCH_DATE, TRANSACTION_TYPE) VALUES (?,?,?)",
            "ABC", "20250101", "TXN_A"
        );
    }

    @Test
    void testPagingMode() throws Exception {
        // Configure FileConfig to use paging on STG_TEST
        FileConfig fc = new FileConfig();
        fc.setTarget("STG_TEST");
        Map<String,String> params = new HashMap<>();
        params.put("format", "jdbc");
        params.put("batchDateParam", "BATCH_DATE");
        params.put("batchDateValue", "20250101");
        params.put("fetchSize", "10");
        params.put("pageSize", "10");
        // no custom query => paging mode
        fc.setParams(params);

        JdbcRecordReader reader = new JdbcRecordReader(fc, dataSource, null);
        ExecutionContext ctx = new ExecutionContext();
        reader.open(ctx);

        // First row (DEFAULT txn)
        Map<String,Object> row1 = reader.read();
        assertNotNull(row1);
        assertEquals("12345", row1.get("ACCT_NUM"));
        // Second row (TXN_A)
        Map<String,Object> row2 = reader.read();
        assertNotNull(row2);
        assertEquals("ABC", row2.get("ACCT_NUM"));
        // No more rows
        assertNull(reader.read());
        reader.close();
    }

    @Test
    void testCursorModeWithCustomSql() throws Exception {
        // Custom SQL that filters only TXN_A
        String sql = """
            SELECT ACCT_NUM, BATCH_DATE, TRANSACTION_TYPE
            FROM STG_TEST
            WHERE TRANSACTION_TYPE = 'TXN_A'
        """;

        FileConfig fc = new FileConfig();
        fc.setTarget("STG_TEST");
        Map<String,String> params = new HashMap<>();
        params.put("format", "jdbc");
        // batchDateParam/value still required for signature but won't be used by cursor
        params.put("batchDateParam", "BATCH_DATE");
        params.put("batchDateValue", "20250101");
        fc.setParams(params);

        JdbcRecordReader reader = new JdbcRecordReader(fc, dataSource, sql);
        ExecutionContext ctx = new ExecutionContext();
        reader.open(ctx);

        // Only the TXN_A row should be returned
        Map<String,Object> row = reader.read();
        assertNotNull(row);
        assertEquals("ABC", row.get("ACCT_NUM"));
        assertNull(reader.read());
        reader.close();
    }
}
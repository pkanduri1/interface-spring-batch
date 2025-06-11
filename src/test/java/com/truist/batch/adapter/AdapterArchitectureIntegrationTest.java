package com.truist.batch.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.truist.batch.model.FileConfig;
import com.truist.batch.reader.GenericReader;

/**
 * Integration test that verifies the complete adapter architecture works end-to-end.
 */
class AdapterArchitectureIntegrationTest {

    private DataSource dataSource;
    private DataSourceAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        // Setup H2 database
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:adapter_test_" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1;MODE=Oracle");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;

        // Create test table
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE TEST_ADAPTER (ID INT, NAME VARCHAR(50))");
        jdbc.update("INSERT INTO TEST_ADAPTER (ID, NAME) VALUES (?, ?)", 1, "Test1");
        jdbc.update("INSERT INTO TEST_ADAPTER (ID, NAME) VALUES (?, ?)", 2, "Test2");

        // Create adapter registry with real adapters
        JdbcDataSourceAdapter jdbcAdapter = new JdbcDataSourceAdapter(dataSource);
        RestApiDataSourceAdapter restAdapter = new RestApiDataSourceAdapter();
        
        registry = new DataSourceAdapterRegistry(java.util.List.of(jdbcAdapter, restAdapter));
    }

    @Test
    void testJdbcAdapterEndToEnd() throws Exception {
        // Create file config for JDBC
        FileConfig fileConfig = new FileConfig();
        fileConfig.setTarget("TEST_ADAPTER");
        Map<String, String> params = new HashMap<>();
        params.put("format", "jdbc");
        params.put("sortKey", "ID");
        fileConfig.setParams(params);

        // Create GenericReader using adapter architecture
        GenericReader reader = new GenericReader(fileConfig, registry);
        
        // Test reading data
        ExecutionContext context = new ExecutionContext();
        reader.open(context);
        
        Map<String, Object> record1 = reader.read();
        assertNotNull(record1);
        assertEquals(1, record1.get("ID"));
        assertEquals("Test1", record1.get("NAME"));
        
        Map<String, Object> record2 = reader.read();
        assertNotNull(record2);
        assertEquals(2, record2.get("ID"));
        assertEquals("Test2", record2.get("NAME"));
        
        // No more records
        assertNull(reader.read());
        
        reader.close();
    }

    @Test
    void testAdapterSelection() {
        // Test that correct adapters are selected for different formats
        assertEquals("JdbcDataSourceAdapter", 
            registry.getAdapter("jdbc").getClass().getSimpleName());
        assertEquals("RestApiDataSourceAdapter", 
            registry.getAdapter("rest").getClass().getSimpleName());
    }

    @Test
    void testSupportedFormats() {
        // Test that all expected formats are supported
        var supportedFormats = registry.getSupportedFormats();
        assertTrue(supportedFormats.contains("jdbc"));
        assertTrue(supportedFormats.contains("rest"));
        assertTrue(supportedFormats.contains("api"));
        assertTrue(supportedFormats.size() >= 3);
    }
}
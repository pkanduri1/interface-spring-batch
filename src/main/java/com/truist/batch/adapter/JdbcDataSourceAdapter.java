package com.truist.batch.adapter;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.truist.batch.model.FileConfig;
import com.truist.batch.reader.JdbcRecordReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DataSourceAdapter implementation for JDBC database sources.
 * 
 * Supports both: - Paging queries (for table scans with WHERE clauses) - Cursor
 * queries (for custom SQL with JOINs, subqueries, etc.)
 * 
 * This adapter wraps the existing JdbcRecordReader functionality.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JdbcDataSourceAdapter implements DataSourceAdapter {

	@Autowired
	private final DataSource dataSource;

	@Override
	public boolean supports(String format) {
		return "jdbc".equalsIgnoreCase(format) || "database".equalsIgnoreCase(format) || "sql".equalsIgnoreCase(format);
	}

	@Override
	public ItemReader<Map<String, Object>> createReader(FileConfig fileConfig) {
		log.info("🗄️  Creating JDBC reader for target: {}", fileConfig.getTarget());

		// Extract custom SQL if provided
		String customSql = fileConfig.getParams().get("query");

		// Create JdbcRecordReader with our existing logic
		return new JdbcRecordReader(fileConfig, dataSource, customSql);
	}

	@Override
	public void validateConfiguration(FileConfig fileConfig) {
		Map<String, String> params = fileConfig.getParams();

		// Validate required parameters
		if (fileConfig.getTarget() == null || fileConfig.getTarget().trim().isEmpty()) {
			throw new IllegalArgumentException("JDBC adapter requires 'target' (table name) to be specified");
		}

		// If no custom query, validate paging parameters
		String customSql = params.get("query");
		if (customSql == null || customSql.trim().isEmpty()) {
			// Paging mode - validate sort key
			String sortKey = params.get("sortKey");
			if (sortKey == null || sortKey.trim().isEmpty()) {
				log.warn("⚠️  No sortKey specified for JDBC paging, defaulting to 'ACCT_NUM'");
			}
		}

		// Validate numeric parameters
		validateNumericParameter(params, "fetchSize", "500");
		validateNumericParameter(params, "pageSize", "1000");

		log.debug("✅ JDBC configuration validation passed for target: {}", fileConfig.getTarget());
	}

	@Override
	public int getPriority() {
		return 100; // High priority for JDBC as it's our primary source type
	}

	/**
	 * Helper to validate numeric parameters with defaults.
	 */
	private void validateNumericParameter(Map<String, String> params, String paramName, String defaultValue) {
		String value = params.getOrDefault(paramName, defaultValue);
		try {
			int numValue = Integer.parseInt(value);
			if (numValue <= 0) {
				throw new IllegalArgumentException(paramName + " must be positive, got: " + numValue);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(paramName + " must be a valid integer, got: " + value);
		}
	}
}
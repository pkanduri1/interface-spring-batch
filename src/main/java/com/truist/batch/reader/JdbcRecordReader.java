package com.truist.batch.reader;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import com.truist.batch.model.FileConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JdbcRecordReader implements ItemStreamReader<Map<String,Object>> {

  private final FileConfig fileConfig;
  private final DataSource dataSource;
  private final String sqlQuery;

  private ItemStreamReader<Map<String,Object>> delegate;

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    // Decide between custom SQL cursor vs. paging reader
    if (sqlQuery != null && !sqlQuery.isBlank()) {
      // Use cursor-based reader for arbitrary queries
      JdbcCursorItemReader<Map<String,Object>> cursor = new JdbcCursorItemReader<>();
      cursor.setDataSource(dataSource);
      cursor.setSql(sqlQuery);
      cursor.setRowMapper(new ColumnMapRowMapper());
      try {
        cursor.afterPropertiesSet();
      } catch (Exception e) {
        throw new ItemStreamException("Failed to initialize cursor reader", e);
      }
      delegate = cursor;
    } else {
      // Fallback to paging based on table name
      JdbcPagingItemReader<Map<String,Object>> paging = new JdbcPagingItemReader<>();
      paging.setDataSource(dataSource);
      paging.setFetchSize(Integer.parseInt(fileConfig.getParams().getOrDefault("fetchSize","500")));
      paging.setPageSize(Integer.parseInt(fileConfig.getParams().getOrDefault("pageSize","1000")));
      paging.setRowMapper(new ColumnMapRowMapper());

      String table     = fileConfig.getTarget();
      String dateParam = fileConfig.getParams().get("batchDateParam");
      SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
      provider.setDataSource(dataSource);
      provider.setSelectClause("SELECT *");
      provider.setFromClause("FROM " + table);
      if (dateParam != null) {
        provider.setWhereClause("WHERE " + dateParam + " = :batchDate");
      }
      
      // ✅ FIX: Use ACCT_NUM instead of ID for sorting (more generic)
      String sortKey = fileConfig.getParams().getOrDefault("sortKey", "ACCT_NUM");
      provider.setSortKey(sortKey);
      
      try {
        paging.setQueryProvider(provider.getObject());
      } catch (Exception e) {
        throw new ItemStreamException("Failed to build query provider", e);
      }
      if (dateParam != null) {
        paging.setParameterValues(Map.of("batchDate", fileConfig.getParams().get("batchDateValue")));
      }
      try {
        paging.afterPropertiesSet();
      } catch (Exception e) {
        throw new ItemStreamException("Failed to initialize paging reader", e);
      }
      delegate = paging;
    }
    // Open the chosen delegate
    if (delegate instanceof ItemStreamReader) {
      ((ItemStreamReader<Map<String,Object>>) delegate).open(executionContext);
    }
  }

  @Override
  public Map<String,Object> read() throws Exception {
    return delegate.read();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
    if (delegate instanceof ItemStreamReader) {
      delegate.update(executionContext);
    }
  }

  @Override
  public void close() throws ItemStreamException {
    if (delegate instanceof ItemStreamReader) {
      delegate.close();
    }
  }
}
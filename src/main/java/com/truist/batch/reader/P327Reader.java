/*
 * package com.truist.batch.reader;
 * 
 * import java.sql.ResultSetMetaData; import java.util.HashMap; import
 * java.util.Map; import java.util.Properties;
 * 
 * import javax.sql.DataSource;
 * 
 * import org.slf4j.Logger; import org.slf4j.LoggerFactory; import
 * org.springframework.batch.item.database.JdbcPagingItemReader; import static
 * org.springframework.batch.item.database.Order.ASCENDING; import
 * org.springframework.batch.item.database.support.OraclePagingQueryProvider;
 * import org.springframework.beans.factory.annotation.Value; import
 * org.springframework.stereotype.Component;
 * 
 * import com.truist.batch.util.YamlPropertyLoaderService;
 * 
 * @Component public class P327Reader extends JdbcPagingItemReader<Map<String,
 * Object>> {
 * 
 * private static final Logger log = LoggerFactory.getLogger(P327Reader.class);
 * 
 * public P327Reader( DataSource dataSource,
 * 
 * @Value("#{stepExecutionContext['rangeStart']}") String rangeStart,
 * 
 * @Value("#{stepExecutionContext['rangeEnd']}") String rangeEnd,
 * 
 * @Value("#{jobParameters['batchDate']}") String batchDate,
 * 
 * @Value("#{jobParameters['sourceSystem']}") String sourceSystem,
 * 
 * @Value("#{jobParameters['jobName']}") String jobName,
 * 
 * @Value("#{jobParameters['pageSize']}") Integer pageSize) {
 * 
 * super();
 * 
 * Properties props = YamlPropertyLoaderService.loadProperties(jobName,
 * sourceSystem);
 * 
 * setDataSource(dataSource); setPageSize(pageSize != null ? pageSize : 500);
 * setName("p327PagingReader");
 * 
 * String baseQuery = props.getProperty("batch-queries.p327-query-1");
 * 
 * OraclePagingQueryProvider queryProvider = new OraclePagingQueryProvider();
 * queryProvider.setSelectClause("SELECT *"); queryProvider.setFromClause("(" +
 * baseQuery + ") temp"); queryProvider.setSortKeys(Map.of("acct_num",
 * ASCENDING)); setQueryProvider(queryProvider);
 * 
 * Map<String, Object> params = new HashMap<>(); params.put("rangeStart",
 * rangeStart); params.put("rangeEnd", rangeEnd); params.put("batchDate",
 * batchDate); setParameterValues(params);
 * 
 * setRowMapper((rs, rowNum) -> { Map<String, Object> row = new HashMap<>();
 * ResultSetMetaData meta = rs.getMetaData(); for (int i = 1; i <=
 * meta.getColumnCount(); i++) { row.put(meta.getColumnName(i).toLowerCase(),
 * rs.getObject(i)); } return row; }); } }
 */
package com.truist.batch.config;

import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

class AcctRangePartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(AcctRangePartitioner.class);

    private DataSource ds;

    private final String tableName;
    private final int gridSize;
    private final TableNameValidator tableNameValidator;
    private final String rangeColumn;
    private static final Set<String> allowedColumns = Set.of("acct_num");

    public AcctRangePartitioner(DataSource ds, String tableName, int gridSize, String rangeColumn, TableNameValidator tableNameValidator
    ) {
        super();
        this.ds = ds;
        this.tableName = tableName;
        this.gridSize = gridSize;
        this.tableNameValidator = tableNameValidator;
        this.rangeColumn = rangeColumn;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        tableNameValidator.validate(tableName);
        if (!allowedColumns.contains(rangeColumn)) {
            throw new IllegalArgumentException("Invalid range column: " + rangeColumn);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        String minMaxQuery = String.format("SELECT MIN(%s), MAX(%s) FROM %s", rangeColumn, rangeColumn, tableName);
        Map<String, Object> minMaxResult = jdbcTemplate.queryForMap(minMaxQuery);

        long min = ((Number) minMaxResult.get("MIN(" + rangeColumn + ")")).longValue();
        long max = ((Number) minMaxResult.get("MAX(" + rangeColumn + ")")).longValue();

        long targetSize = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> result = new java.util.HashMap<>();

        long start = min;
        long end = start + targetSize - 1;

        for (int i = 0; i < gridSize; i++) {
            if (end > max) {
                end = max;
            }

            ExecutionContext context = new ExecutionContext();
            context.putLong("minValue", start);
            context.putLong("maxValue", end);
            context.putString("tableName", tableName);
            context.putString("rangeColumn", rangeColumn);

            result.put("partition" + i, context);

            start += targetSize;
            end += targetSize;
        }

        return result;
    }

}

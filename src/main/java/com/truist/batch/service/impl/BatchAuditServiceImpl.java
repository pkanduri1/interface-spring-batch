package com.truist.batch.service.impl;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.truist.batch.service.BatchAuditService;

@Service
public class BatchAuditServiceImpl implements BatchAuditService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void logJobStart(String jobName, String sourceSystem, Timestamp batchDate, String outputFileName) {
        jdbcTemplate.update(
            "INSERT INTO batch_audit_log (job_name, source_system, batch_date, file_name, start_time, status) VALUES (?, ?, ?, ?, ?, ?)",
            jobName, sourceSystem, batchDate, outputFileName, new Timestamp(System.currentTimeMillis()), "STARTED"
        );
    }

    @Override
    public void logJobEnd(String jobName, String sourceSystem, Timestamp batchDate, String outputFileName, int recordCount, String status) {
        jdbcTemplate.update(
            "UPDATE batch_audit_log SET end_time = ?, record_count = ?, status = ? WHERE job_name = ? AND source_system = ? AND batch_date = ? AND file_name = ?",
            new Timestamp(System.currentTimeMillis()), recordCount, status, jobName, sourceSystem, batchDate, outputFileName
        );
    }

    @Override
    public void logJobFailure(String jobName, String sourceSystem, Timestamp batchDate, String outputFileName, String errorMessage) {
        jdbcTemplate.update(
            "UPDATE batch_audit_log SET end_time = ?, status = ?, error_message = ? WHERE job_name = ? AND source_system = ? AND batch_date = ? AND file_name = ?",
            new Timestamp(System.currentTimeMillis()), "FAILED", errorMessage, jobName, sourceSystem, batchDate, outputFileName
        );
    }
}
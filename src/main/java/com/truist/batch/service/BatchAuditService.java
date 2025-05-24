package com.truist.batch.service;

import java.sql.Timestamp;

public interface BatchAuditService {
    void logJobStart(String jobName, String sourceSystem, Timestamp batchDate, String outputFileName);
    void logJobEnd(String jobName, String sourceSystem, Timestamp batchDate, String outputFileName, int recordCount, String status);
    void logJobFailure(String jobName, String sourceSystem, Timestamp batchDate, String outputFileName, String errorMessage);
}

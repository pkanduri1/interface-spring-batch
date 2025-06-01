package com.truist.batch.listener;

import java.util.Date;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GenericStepListener implements StepExecutionListener {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    log.info("Starting step '{}' for partition key [{}]",
        stepExecution.getStepName(),
        stepExecution.getExecutionContext().getString("transactionType", "default"));
    // returning null as per interface contract
    return;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.info("Completed step '{}' for partition key [{}]: read={}, written={}, skipped={}, commits={}",
        stepExecution.getStepName(),
        stepExecution.getExecutionContext().getString("transactionType", "default"),
        stepExecution.getReadCount(),
        stepExecution.getWriteCount(),
        stepExecution.getSkipCount(),
        stepExecution.getCommitCount());
    // Insert audit record into BATCH_STEP_AUDIT
    String sql = "INSERT INTO BATCH_STEP_AUDIT (JOB_NAME, STEP_NAME, PARTITION_KEY, READ_COUNT, WRITE_COUNT, SKIP_COUNT, COMMIT_COUNT, START_TIME, END_TIME) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
    String stepName = stepExecution.getStepName();
    String partitionKey = stepExecution.getExecutionContext().getString("transactionType", "default");
    int readCount = (int)stepExecution.getReadCount();
    int writeCount = (int)stepExecution.getWriteCount();
    int skipCount = (int)stepExecution.getSkipCount();
    int commitCount = (int)stepExecution.getCommitCount();
    Date startTime = stepExecution.getStartTime() != null ? java.sql.Timestamp.valueOf(stepExecution.getStartTime()) : null;
    Date endTime = stepExecution.getEndTime() != null ? java.sql.Timestamp.valueOf(stepExecution.getEndTime()) : null;
    jdbcTemplate.update(sql, jobName, stepName, partitionKey, readCount, writeCount, skipCount, commitCount, startTime, endTime);
    return stepExecution.getExitStatus();
  }
}

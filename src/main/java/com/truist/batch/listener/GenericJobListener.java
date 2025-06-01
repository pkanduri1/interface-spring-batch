package com.truist.batch.listener;


import java.sql.Timestamp;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;




@Component
@Slf4j
public class GenericJobListener implements JobExecutionListener {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting job '{}' with parameters {}", 
            jobExecution.getJobInstance().getJobName(), 
            jobExecution.getJobParameters().toString());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();
        
        long totalReadCount = 0;
        long totalWriteCount = 0;
        long totalSkipCount = 0;
        
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            // ✅ Only count data processing steps, not tasklet steps
            if (stepExecution.getStepName().contains("WorkerStep")) {
                totalReadCount += stepExecution.getReadCount();
                totalWriteCount += stepExecution.getWriteCount();
                totalSkipCount += stepExecution.getSkipCount();
            }
        }
        
        log.info("Job '{}' finished with status {}. Read={}, Written={}, Skipped={}", 
                jobName, status, totalReadCount, totalWriteCount, totalSkipCount);
    }
}

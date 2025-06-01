package com.truist.batch.monitor;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import com.truist.batch.config.DynamicBatchConfigLoader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleExecutionMonitor implements JobExecutionListener {
    
    private final DynamicBatchConfigLoader configLoader;
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        var params = jobExecution.getJobParameters();
        log.info("▶️ Starting job: sourceSystem={}, jobName={} (Thread: {})", 
            params.getString("sourceSystem"),
            params.getString("jobName"),
            Thread.currentThread().getName());
        log.info("📁 Cached configs: {}", configLoader.getCachedSourceSystems());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        var params = jobExecution.getJobParameters();
        log.info("✅ Completed job: sourceSystem={}, jobName={}, status={}", 
            params.getString("sourceSystem"),
            params.getString("jobName"),
            jobExecution.getStatus());
            
        // Simple step summary
        long totalRead = jobExecution.getStepExecutions().stream()
            .mapToLong(step -> step.getReadCount())
            .sum();
        long totalWritten = jobExecution.getStepExecutions().stream()
            .mapToLong(step -> step.getWriteCount())
            .sum();
            
        log.info("📊 Total processed: read={}, written={}", totalRead, totalWritten);
    }
}
package com.truist.batch.monitor;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.truist.batch.config.DynamicBatchConfigLoader;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//@Component
@RequiredArgsConstructor
@Slf4j
public class ParallelExecutionMonitor {
    
    private final DynamicBatchConfigLoader configLoader;
    private final JobExplorer jobExplorer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Starting parallel execution monitor...");
        // Log status every 30 seconds
        scheduler.scheduleAtFixedRate(this::logStatus, 10, 30, TimeUnit.SECONDS);
    }
    
    private void logStatus() {
        try {
            Set<JobExecution> runningJobs = jobExplorer.findRunningJobExecutions("genericJob");
            
            if (!runningJobs.isEmpty()) {
                log.info("=== Parallel Execution Status ===");
                log.info("Running jobs: {}", runningJobs.size());
                log.info("Cached source systems: {}", configLoader.getCachedSourceSystems());
                
                for (JobExecution job : runningJobs) {
                    var params = job.getJobParameters();
                    log.info("Job {}: sourceSystem={}, jobName={}, status={}", 
                        job.getId(),
                        params.getString("sourceSystem"),
                        params.getString("jobName"),
                        job.getStatus());
                }
                log.info("================================");
            }
        } catch (Exception e) {
            log.debug("Error monitoring job status: {}", e.getMessage());
        }
    }
    
    // Clean shutdown
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
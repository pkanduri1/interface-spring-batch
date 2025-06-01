package com.truist.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableBatchProcessing
@Slf4j
public class TaskExecutorConfig {

    @Autowired
    private Environment environment;

    @Bean
    public TaskExecutor taskExecutor() {
        if (isDebugMode()) {
            // ✅ SYNCHRONOUS EXECUTOR - No thread pool, no parallel execution
            log.info("🐛 DEBUG MODE: Using SyncTaskExecutor (truly single-threaded)");
            return new SyncTaskExecutor();
        } else {
            // Normal parallel execution
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(10);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("batch-");
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(60);
            executor.initialize();
            log.info("🚀 PRODUCTION MODE: Using ThreadPoolTaskExecutor (parallel execution)");
            return executor;
        }
    }
    
    /**
     * Detects if we're running in debug mode
     */
    private boolean isDebugMode() {
        boolean debugJVM = java.lang.management.ManagementFactory.getRuntimeMXBean()
            .getInputArguments().toString().contains("-agentlib:jdwp");
        
        boolean debugProp = "true".equals(System.getProperty("debug.batch"));
        boolean debugEnv = "true".equals(System.getenv("DEBUG_BATCH"));
        boolean debugProfile = java.util.Arrays.asList(environment.getActiveProfiles()).contains("debug");
        boolean debugConfig = environment.getProperty("batch.debug.enabled", Boolean.class, false);
        boolean ideDebug = System.getProperty("java.class.path").contains("idea_rt.jar");
        
        // Also check if gridSize=1 was explicitly set
        boolean singleThreaded = environment.getProperty("batch.gridSize", Integer.class, 10) == 1;
        
        return debugJVM || debugProp || debugEnv || debugProfile || debugConfig || ideDebug || singleThreaded;
    }
}
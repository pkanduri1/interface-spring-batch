package com.truist.batch.model;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Binds all batch engine settings from application.yml under the 'batch' prefix.
 * <ul>
 *   <li>gridSize: thread-pool size for file partitioning</li>
 *   <li>chunkSize: number of items per transaction chunk</li>
 *   <li>sources: definitions of source systems, jobs, and file configurations</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "batch")
public class BatchJobProperties {

    /** 
     * The number of threads to use when partitioning files for parallel execution.
     * Configurable via 'batch.gridSize' in application.yml.
     */
    /** Thread pool size for partition step */
    private int gridSize = 4;

    /**
     * The size of each processing chunk (number of records per commit).
     * Configurable via 'batch.chunkSize' in application.yml.
     */
    /** Chunk size for processing items */
    private int chunkSize = 100;

    /**
     * A map of source system identifiers (e.g. 'shaw', 'hr') to their configuration.
     * Each entry corresponds to a block under 'batch.sources' in application.yml.
     */
    /** Map of source system configurations */
    private Map<String, SourceSystemConfig> sources;

    public SourceSystemConfig getSourceSystem(String name) {
        SourceSystemConfig cfg = sources.get(name);
        if (cfg == null) {
            throw new IllegalArgumentException("Unknown source system: " + name);
        }
        return cfg;
    }

    public JobConfig getJobConfig(String sourceSystem, String jobName) {
        SourceSystemConfig systemConfig = getSourceSystem(sourceSystem);
        JobConfig job = systemConfig.getJobs().get(jobName);
        if (job == null) {
            throw new IllegalArgumentException(
                "Unknown job '" + jobName + "' for source system '" + sourceSystem + "'");
        }
        return job;
    }
}

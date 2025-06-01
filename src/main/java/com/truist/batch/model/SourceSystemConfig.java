package com.truist.batch.model;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

/**
 * Configuration for a single source system.
 */
@Data
public class SourceSystemConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Map of jobName to its configuration */
    private Map<String, JobConfig> jobs;
}

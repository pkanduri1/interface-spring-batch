package com.truist.batch.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for a specific batch job under a source system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobConfig {
    private String id;
    private String sourceSystemId;
    private String jobName;
    private String description;
    private String inputPath;
    private String outputPath;
    private String querySql;
    private boolean enabled;
    private LocalDateTime created;
    private List<String> transactionTypes;
}
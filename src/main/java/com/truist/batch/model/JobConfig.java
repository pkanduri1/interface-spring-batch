package com.truist.batch.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Configuration for a specific batch job under a source system.
 */
@Data
public class JobConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Files to process for this job */
    private List<FileConfig> files;

    /** If true, each file contains multiple transaction types */
    private boolean multiTxn;
}
package com.truist.batch.model;

import lombok.Data;

@Data
public class BatchSettings{
    private int chunkSize;
    private int pageSize;
    private int gridSize;
    private int retryLimit;
}
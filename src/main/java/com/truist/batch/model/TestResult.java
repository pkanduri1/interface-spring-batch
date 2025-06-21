package com.truist.batch.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {
    private boolean success;
    private String message;
    private List<String> sampleOutput;
    private long executionTimeMs;
}
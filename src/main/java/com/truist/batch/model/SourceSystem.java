package com.truist.batch.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceSystem {
    private String id;
    private String name;
    private String type; // "ORACLE", "SQLSERVER", "FILE", "API"
    private String description;
    private boolean enabled;
    private int jobCount;
    private LocalDateTime lastModified;
    private Map<String, String> connectionProperties;
}
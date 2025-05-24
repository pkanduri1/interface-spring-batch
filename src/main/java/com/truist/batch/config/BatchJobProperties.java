package com.truist.batch.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.truist.batch.model.BatchSettings;
import com.truist.batch.model.SourceTables;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix="batch")
public class BatchJobProperties{

    private BatchSettings settings;
    private List<String> allowedTables;
    private Map<String, SourceTables> tables;

}
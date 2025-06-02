package com.truist.batch.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.truist.batch.model.EnhancedFieldMapping;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.TargetDefinition;
import com.truist.batch.model.TargetField;
import com.truist.batch.service.FieldTransformationService;
import com.truist.batch.service.SourceMappingService;
import com.truist.batch.service.TargetDefinitionService;
import com.truist.batch.util.FormatterUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced processor using centralized target definitions and separated transformation logic
 */
@StepScope
@RequiredArgsConstructor
@Component
@Slf4j
public class EnhancedGenericProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final FileConfig fileConfig;
    private final TargetDefinitionService targetDefinitionService;
    private final SourceMappingService sourceMappingService;
    private final FieldTransformationService transformationService;

    @Override
    public Map<String, Object> process(Map<String, Object> item) throws Exception {
        try {
            String targetName = determineTargetName(item);
            TargetDefinition targetDef = targetDefinitionService.getTargetDefinition(targetName);
            
            String transactionType = getTransactionType(item);
            Map<String, Object> output = new LinkedHashMap<>();
            
            for (TargetField targetField : targetDef.getFields()) {
                String fieldName = targetField.getName();
                
                EnhancedFieldMapping sourceMapping = sourceMappingService.getFieldMapping(
                    fileConfig.getSourceSystem(),
                    targetName,
                    fieldName,
                    transactionType
                );
                
                String transformedValue = transformationService.transformField(
                    item, sourceMapping, targetField.getDefaultValue()
                );
                
                String formattedValue = FormatterUtil.formatValue(transformedValue, targetField);
                output.put(fieldName, formattedValue);
            }
            
            return output;
            
        } catch (Exception e) {
            log.error("Failed to process item: {}", item, e);
            throw e;
        }
    }
    
    private String determineTargetName(Map<String, Object> item) {
        String transactionType = getTransactionType(item);
        return fileConfig.getJobName() + "-" + transactionType; // "atoctran-200"
    }
    
    private String getTransactionType(Map<String, Object> item) {
        String transactionType = (String) item.get("transactionType");
        if (transactionType == null) {
            transactionType = fileConfig.getTransactionType();
        }
        return transactionType != null ? transactionType : "default";
    }
}
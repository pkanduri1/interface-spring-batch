package com.truist.batch.processor;


import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.YamlMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic ItemProcessor implementation that maps an input record (as a Map of raw values)
 * into an ordered Map of transformed, fixed-width strings based on YAML-defined field mappings.
 *
 * This processor:
 *  1. Extracts the transactionType (if any) from the record context.
 *  2. Retrieves the corresponding YamlMapping document for the given template and transactionType.
 *  3. Sorts the FieldMapping rules by their configured targetPosition.
 *  4. Applies each FieldMapping (constant, source, composite, conditional) via the mappingService,
 *     which pads, formats, and defaults the values as defined.
 *  5. Builds a LinkedHashMap to preserve the exact field order for downstream writers.
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GenericProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final FileConfig fileConfig;
    private final YamlMappingService mappingService;

    /**
     * Processes one input item by applying the appropriate mapping rules.
     *
     * @param item a Map of source field names to raw values for a single record
     * @return a LinkedHashMap of target field names to their transformed string values,
     *         preserving the fixed-width order defined by targetPosition
     * @throws Exception if there is an error loading mappings or transforming fields
     */
    @Override
    public Map<String, Object> process(Map<String, Object> item) throws Exception {
    	log.debug("🔍 Input item: {}", item);
        // 1) Extract transactionType from the record, or null if not provided
        String txnType = fileConfig.getTransactionType();
        // 2) Fetch the YAML mapping document matching template and transactionType
        YamlMapping mapping = mappingService.getMapping(fileConfig.getTemplate(), txnType);
        // 3) Extract and sort the FieldMapping entries by targetPosition to enforce output order
        List<FieldMapping> fields = mapping.getFields().entrySet().stream()
            .map(Map.Entry::getValue)
            .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
            .collect(Collectors.toList());
        // 4) Iterate over each FieldMapping, transform the raw value (padding/formatting), and collect into output
        Map<String, Object> output = new LinkedHashMap<>();
        for (FieldMapping m : fields) {
            String value = mappingService.transformField(item, m);
            log.debug("Field {}: '{}' -> '{}'", m.getTargetField(), 
                    item.get(m.getSourceField()), value);
            output.put(m.getTargetField(), value != null ? value : m.getDefaultValue());
        }
        return output;
    }
}
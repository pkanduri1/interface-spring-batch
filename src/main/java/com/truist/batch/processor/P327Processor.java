/*
 * package com.truist.batch.processor;
 * 
 * import java.util.HashMap; import java.util.List; import java.util.Map; import
 * java.util.Map.Entry; import java.util.Properties;
 * 
 * import org.slf4j.Logger; import org.slf4j.LoggerFactory; import
 * org.springframework.batch.core.configuration.annotation.StepScope; import
 * org.springframework.batch.item.ItemProcessor; import
 * org.springframework.beans.factory.annotation.Value; import
 * org.springframework.context.annotation.Configuration;
 * 
 * import com.truist.batch.exception.ItemProcessingException; import
 * com.truist.batch.mapping.YamlMappingService; import
 * com.truist.batch.model.FieldMapping; import
 * com.truist.batch.util.ErrorLogger; import
 * com.truist.batch.util.YamlPropertyLoaderService;
 * 
 *//**
	 * The P327Processor class is responsible for processing each row of data read
	 * by the reader. It applies transformations defined in YAML field mappings and
	 * constructs a target row as a Map of field names to transformed values.
	 */
/*
 * @Configuration
 * 
 * @StepScope public class P327Processor implements ItemProcessor<Map<String,
 * Object>, Map<String, String>> {
 * 
 * private static final Logger logger =
 * LoggerFactory.getLogger(P327Processor.class);
 * 
 * // Service for loading and applying YAML-based field mappings private final
 * YamlMappingService yamlMappingService;
 * 
 * // Job name and source system, used to identify the YAML configuration
 * private final String jobName; private final String sourceSystem;
 * 
 * // List of field mappings loaded from the YAML file private
 * List<Entry<String, FieldMapping>> fieldMappings;
 * 
 * // Properties specific to the job and source system private Properties
 * p327PropsBySrc;
 * 
 *//**
	 * Constructs a new P327Processor which processes rows based on YAML-defined
	 * field mappings.
	 *
	 * @param yamlMappingService the service used to load and apply field mappings
	 * @param sourceSystem       the source system name (e.g., "shaw", "hr")
	 * @param jobName            the job name associated with the YAML and
	 *                           properties configuration
	 */
/*
 * public P327Processor(YamlMappingService
 * yamlMappingService, @Value("#{jobParameters['sourceSystem']}") String
 * sourceSystem, @Value("#{jobParameters['jobName']}") String jobName) {
 * this.yamlMappingService = yamlMappingService; this.jobName = jobName;
 * this.sourceSystem = sourceSystem; loadFieldMappings(); }
 * 
 *//**
	 * Loads field mappings from a YAML file and job-specific properties based on
	 * the source system and job name. This method initializes the fieldMappings and
	 * job-specific properties.
	 */
/*
 * private void loadFieldMappings() { // Construct the path to the YAML file
 * based on job name and source system String ymlPath = jobName + "/" +
 * sourceSystem + "/" + jobName + ".yml";
 * 
 * // Load field mappings from the YAML file this.fieldMappings =
 * yamlMappingService.loadFieldMappings(ymlPath);
 * 
 * // Load job-specific properties from the YAML file this.p327PropsBySrc =
 * YamlPropertyLoaderService.loadProperties(jobName, sourceSystem); }
 * 
 *//**
	 * Processes a single item (a row of data) by applying transformation rules
	 * defined in the field mappings. Constructs a target row as a Map of field
	 * names to transformed string values. Logs any transformation errors and writes
	 * failed rows to a log file before throwing an exception.
	 *
	 * @param item the input row from the reader
	 * @return a Map representing the transformed output row
	 * @throws Exception if any error occurs during field transformation
	 *//*
		 * @Override public Map<String, String> process(Map<String, Object> item) throws
		 * Exception { // Initialize the target row to store transformed values
		 * Map<String, String> targetRow = new HashMap<>();
		 * 
		 * // Iterate over each field mapping and apply the transformation for
		 * (Map.Entry<String, FieldMapping> entry : fieldMappings) { String targetField
		 * = entry.getKey(); FieldMapping mapping = entry.getValue();
		 * 
		 * try { // Transform the field value using the mapping service String
		 * transformedValue = yamlMappingService.transformField(item, mapping);
		 * 
		 * // Add the transformed value to the target row targetRow.put(targetField,
		 * transformedValue); } catch (Exception e) { // Log the error and write the
		 * failed row to the error log
		 * logger.error("Failed to process field [{}] for row: {}. Error: {}",
		 * targetField, item, e.getMessage(), e);
		 * 
		 * ErrorLogger.logError( item, sourceSystem, jobName, e.getMessage(),
		 * p327PropsBySrc.getProperty("batch.skip.skip-log-path") );
		 * 
		 * // Throw an exception to indicate processing failure throw new
		 * ItemProcessingException("Error processing row: " + item, e); } }
		 * 
		 * // Return the transformed row return targetRow; } }
		 */
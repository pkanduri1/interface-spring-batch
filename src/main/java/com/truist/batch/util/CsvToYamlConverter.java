package com.truist.batch.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.Condition;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;

/**
 * Utility class to convert CSV files to YAML format based on predefined mappings.
 * The CSV file is expected to have a specific structure defining field mappings,
 * conditions, and transformations.
 */
public class CsvToYamlConverter {

    /**
     * Converts a CSV file to a YAML file.
     *
     * @param csvPath Path to the input CSV file.
     * @param yamlOutputPath Path to the output YAML file.
     * @param fileType The type of file being processed, this will be included in the YAML output.
     * @throws Exception if any error occurs during file reading, parsing, or writing.
     */
    public static void convert(String csvPath, String yamlOutputPath, String fileType) throws Exception {
        // Map to store FieldMapping objects, keyed by field name.
        Map<String,FieldMapping> fieldMappings = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            // Read the header line to understand CSV structure.
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");

            // Create a map of header names to their column indices for easy access.
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim(), i);
            }

            String line;
            int lineNumber = 1; // Header is line 1
            // Process each line of the CSV file.
            while ((line = reader.readLine()) != null) {
                lineNumber++; // Increment for current data row
                String[] tokens = null;
                try {
                    // Split the line into tokens based on comma delimiter.
                    // The -1 argument ensures that trailing empty strings are also included.
                    tokens = line.split(",", -1);
                    FieldMapping field = new FieldMapping();

                    // Map CSV columns to FieldMapping object properties.
                    field.setFieldName(tokens[headerMap.get("Field Name")].trim());
                field.setTargetPosition(Integer.parseInt(tokens[headerMap.get("Target Position")].trim()));
                field.setLength(Integer.parseInt(tokens[headerMap.get("Length")].trim()));
                field.setDataType(tokens[headerMap.get("Data Type")].trim());
                field.setFormat(tokens[headerMap.get("Format")].trim());
                field.setPad(tokens[headerMap.get("Pad")].trim());
                field.setTransformationType(tokens[headerMap.get("Transformation Type")].trim());
                field.setTransform(tokens[headerMap.get("Transform")].trim());
                field.setDefaultValue(tokens[headerMap.get("Default Value")].trim());
                field.setTargetField(tokens[headerMap.get("Target Field Name")].trim());
                field.setSourceField(tokens[headerMap.get("Source Field Name")].trim());

                // Handle conditional logic (if, else if, else).
                Condition condition = new Condition();
                condition.setIfExpr(tokens[headerMap.get("If Expression")].trim());
                condition.setElseExpr(tokens[headerMap.get("Else")].trim());

                // Handle multiple "else if" expressions, separated by pipe character.
                String elseIfExprs = tokens[headerMap.get("Else If Expression")].trim();
                if (!elseIfExprs.isEmpty()) {
                    List<Condition> elseIfConditions = new ArrayList<>();
                    for (String elseif : elseIfExprs.split("\\|")) {
                        Condition elseifCondition = new Condition();
                        elseifCondition.setIfExpr(elseif.trim());
                        elseIfConditions.add(elseifCondition);
                    }
                    condition.setElseIfExprs(elseIfConditions);
                }

                field.setConditions(Collections.singletonList(condition));

                // Default value and padding logic.
                // Set default pad character based on pad direction if not specified.
                if (field.getPadChar() == null || field.getPadChar().isEmpty()) {
                    field.setPadChar("left".equalsIgnoreCase(field.getPad()) ? "0" : " ");
                }

                // Set default value to a single space if it's empty.
                if (field.getDefaultValue() == null || field.getDefaultValue().isEmpty()) {
                    field.setDefaultValue(" ");
                }

                    // Store the configured FieldMapping object.
                    fieldMappings.put(field.getFieldName(), field);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Error parsing number on line " + lineNumber + " (Content: \"" + line + "\"): " + e.getMessage(), e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    int tokenCount = (tokens != null) ? tokens.length : 0; // Defensive token count
                    throw new RuntimeException("Error processing columns on line " + lineNumber + " (Content: \"" + line + "\"). Expected " + headers.length + " columns based on header, but found " + tokenCount + ". Error: " + e.getMessage(), e);
                } catch (Exception e) { // Catch any other unexpected errors during row processing
                    throw new RuntimeException("Error processing CSV line " + lineNumber + " (Content: \"" + line + "\"): " + e.getMessage(), e);
                }
            }
        }

        // Prepare the top-level YAML structure.
        YamlMapping yamlMapping = new YamlMapping();
        yamlMapping.setFileType(fileType);
        yamlMapping.setFields(fieldMappings);

        // YAML Serialization: Convert the YamlMapping object to YAML format.
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (FileWriter writer = new FileWriter(Paths.get(yamlOutputPath).toFile())) {
            mapper.writeValue(writer, yamlMapping);
        }
    }

    /**
     * Main method to run the CSV to YAML conversion from the command line.
     *
     * @param args Command line arguments:
     *             args[0] - Path to the input CSV file.
     *             args[1] - Path to the output YAML file.
     *             args[2] - The file type.
     * @throws Exception if any error occurs during conversion.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java CsvToYamlConverter <input.csv> <output.yml> <fileType>");
            return;
        }
        convert(args[0], args[1], args[2]);
    }
}

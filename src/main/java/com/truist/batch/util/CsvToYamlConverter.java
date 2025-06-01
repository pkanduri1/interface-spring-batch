package com.truist.batch.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.Condition;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;

/**
 * Utility class to convert CSV files to YAML format based on predefined
 * mappings. The CSV file is expected to have a specific structure defining
 * field mappings, conditions, and transformations.
 */
public class CsvToYamlConverter {

    /**
     * Converts a CSV file to a YAML file.
     *
     * @param fileType The type of file being processed, this will be included
     * in the YAML output.
     * @param sourceSystem The source system of the file, used to locate the CSV.
     * @throws Exception if any error occurs during file reading, parsing, or
     * writing.
     */
    public static void convert(String fileType, String sourceSystem) throws Exception {
        String csvPath = "/" + fileType + "/" + sourceSystem + "/mappings/" + fileType + "-" + sourceSystem + "-mapping.csv";
        String yamlOutputPath = "src/main/resources/" + fileType + "/" + sourceSystem + "/" + fileType + ".yml";

        Map<String, Map<String, FieldMapping>> allMappings = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        CsvToYamlConverter.class.getResourceAsStream(csvPath)
                )
        )) {
            // Read the header line to understand CSV structure.
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");

            // Create a map of sanitized header names to their column indices for easy access.
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(normalizeKey(headers[i]), i);
            }

            // Determine optional column indices (may not exist in all CSVs)
            int idxTxnType    = headerMap.getOrDefault(normalizeKey("transactiontype"), -1);
            int idxIfExpr     = headerMap.getOrDefault(normalizeKey("if"), -1);
            int idxElseExpr   = headerMap.getOrDefault(normalizeKey("else"), -1);
            int idxElseIfExpr = headerMap.getOrDefault(normalizeKey("elseif"), -1);

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

                    int idxFieldName = headerMap.get(normalizeKey("FieldName"));
                    int idxTargetPosition = headerMap.get(normalizeKey("TargetPosition"));
                    int idxLength = headerMap.get(normalizeKey("Length"));
                    int idxDataType = headerMap.get(normalizeKey("DataType"));
                    int idxFormat = headerMap.get(normalizeKey("Format"));
                    int idxPad = headerMap.get(normalizeKey("Pad"));
                    int idxTransformationType = headerMap.get(normalizeKey("TransformationType"));
                    int idxTransform = headerMap.get(normalizeKey("Transform"));
                    int idxDefaultValue = headerMap.get(normalizeKey("DefaultValue"));
                    int idxTargetField = headerMap.get(normalizeKey("TargetFieldName"));
                    int idxSourceField = headerMap.get(normalizeKey("SourceFieldName"));

                    FieldMapping field = new FieldMapping();

                    // Map CSV columns to FieldMapping object properties.
                    field.setFieldName(tokens[idxFieldName].trim().toLowerCase());
                    field.setTargetPosition(Integer.parseInt(tokens[idxTargetPosition].trim()));
                    field.setLength(Integer.parseInt(tokens[idxLength].trim()));
                    field.setDataType(tokens[idxDataType].trim().toLowerCase());
                    field.setFormat(tokens[idxFormat].trim().toLowerCase());
                    field.setPad(tokens[idxPad].trim().toLowerCase());
                    field.setTransformationType(tokens[idxTransformationType].trim().toLowerCase());
                    field.setTransform(tokens[idxTransform].trim().toLowerCase());
                    field.setDefaultValue(tokens[idxDefaultValue].trim());
                    field.setTargetField(tokens[idxTargetField].trim().toLowerCase());
                    field.setSourceField(tokens[idxSourceField].trim().toLowerCase());

                    // Handle conditional logic (if, else if, else).
                    Condition condition = new Condition();
                    if (idxIfExpr >= 0 && idxIfExpr < tokens.length) {
                        condition.setIfExpr(tokens[idxIfExpr].trim());
                    }
                    if (idxElseExpr >= 0 && idxElseExpr < tokens.length) {
                        condition.setElseExpr(tokens[idxElseExpr].trim());
                    }
                    if (idxElseIfExpr >= 0 && idxElseIfExpr < tokens.length) {
                        String elseIfExprs = tokens[idxElseIfExpr].trim();
                        if (!elseIfExprs.isEmpty()) {
                            List<Condition> elseIfConditions = new ArrayList<>();
                            for (String elseif : elseIfExprs.split("\\|")) {
                                Condition elseifCondition = new Condition();
                                elseifCondition.setIfExpr(elseif.trim());
                                elseIfConditions.add(elseifCondition);
                            }
                            condition.setElseIfExprs(elseIfConditions);
                        }
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

                    // Determine transaction type for grouping
                    String txnType = "default";
                    if (idxTxnType >= 0 && idxTxnType < tokens.length) {
                        String rawTxn = tokens[idxTxnType].trim();
                        if (!rawTxn.isEmpty()) {
                            txnType = sanitize(rawTxn);
                        }
                    }
                    allMappings.computeIfAbsent(txnType, k -> new LinkedHashMap<>())
                            .put(field.getFieldName(), field);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Error parsing number on line " + lineNumber + " (Content: \"" + line + "\"): " + e.getMessage(), e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    int tokenCount = (tokens != null) ? tokens.length : 0; // Defensive token count
                    throw new RuntimeException("Error processing columns on line " + lineNumber + " (Content: \"" + line + "\"). Token count mismatch detected. Error: " + e.getMessage(), e);
                } catch (Exception e) { // Catch any other unexpected errors during row processing
                    throw new RuntimeException("Error processing CSV line " + lineNumber + " (Content: \"" + line + "\"): " + e.getMessage(), e);
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        File outputFile = Paths.get(yamlOutputPath).toFile();
        try (FileWriter writer = new FileWriter(outputFile)) {
            boolean first = true;
            for (Map.Entry<String, Map<String, FieldMapping>> entry : allMappings.entrySet()) {
                YamlMapping yamlMapping = new YamlMapping();
                yamlMapping.setFileType(fileType);
                yamlMapping.setTransactionType(entry.getKey());
                yamlMapping.setFields(entry.getValue());
                if (!first) {
                    writer.write("---\n");
                }
                first = false;
                writer.write(mapper.writeValueAsString(yamlMapping));
            }
        }
    }

    /**
     * Sanitizes a string by: - Removing non-alphanumeric characters except
     * underscore, hyphen, and dot - Replacing spaces and unsafe characters with
     * underscores - Collapsing multiple underscores - Trimming leading/trailing
     * underscores - Converting to lowercase
     *
     * @param input The raw input string.
     * @return A safe, lowercase, underscore-normalized version of the input.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }

        return input
                .toLowerCase()
                .replaceAll("[^a-z0-9_\\-\\.]", "_") // Replace non-safe characters with _
                .replaceAll("_+", "_") // Collapse multiple underscores
                .replaceAll("^_|_$", "");            // Trim leading/trailing underscores
    }

    /**
     * Normalizes a CSV header key by lowercasing and removing all non-alphanumeric characters.
     * @param key the raw header key
     * @return normalized key, e.g. "Field-Name" -> "fieldname"
     */
    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Main method to run the CSV to YAML conversion from the command line.
     *
     * @param args Command line arguments: args[0] - fileType, args[1] - sourceSystem.
     * @throws Exception if any error occurs during conversion.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java CsvToYamlConverter <fileType> <sourceSystem>");
            return;
        }
        convert(args[0], args[1]);
    }
}

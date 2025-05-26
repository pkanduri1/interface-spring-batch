package com.truist.batch.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList; 
import java.util.Collections; 
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import com.truist.batch.model.Condition;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;
import com.truist.batch.util.FormatterUtil;

@Service
public class YamlMappingService {

    /**
     * Loads field mappings from a YAML file located at the given path in the classpath.
     * The mappings are returned as a list of entries, sorted by the target position defined in the YAML.
     *
     * @param yamlPath the path to the YAML file (e.g., "p327/shaw/p327.yml")
     * @return a sorted list of field mapping entries (Map.Entry<String, FieldMapping>)
     * @throws RuntimeException if the YAML file cannot be loaded or parsed.
     */
    public List<Map.Entry<String, FieldMapping>> loadFieldMappings(String yamlPath) {
        try (InputStream input = new ClassPathResource(yamlPath).getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false); // Disallow duplicate field names in YAML
            Yaml yml = new Yaml(new Constructor(YamlMapping.class, options));
            YamlMapping yamlMapping = yml.load(input);
            
            if (yamlMapping == null || yamlMapping.getFields() == null) {
                throw new RuntimeException("YAML file at path: " + yamlPath + " is empty or not structured correctly (missing 'fields' map).");
            }

            return yamlMapping.getFields().entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> e.getValue().getTargetPosition()))
                    .collect(Collectors.toList());
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to load YAML from classpath path: " + yamlPath, ioe);
        } catch (Exception e) { // Catch other potential parsing errors
            throw new RuntimeException("Failed to parse YAML from classpath path: " + yamlPath, e);
        }
    }

    /**
     * Applies transformation logic to a single field from an input data row based on its {@link FieldMapping} definition.
     * This method orchestrates various transformation types like constant, source-based, composite, or conditional.
     * The final value is then padded according to the field's length and padding rules.
     *
     * @param row the input data row as a Map of field names to values.
     * @param mapping the {@link FieldMapping} definition for the target field.
     * @return the transformed and formatted value as a String.
     */
    public String transformField(Map<String, Object> row, FieldMapping mapping) {
        String value = "";

        switch (Optional.ofNullable(mapping.getTransformationType()).orElse("").toLowerCase()) {
            case "constant":
                value = mapping.getValue(); // 'value' field in YAML used for constants
                break;
            case "source":
                value = resolveValue(mapping.getSourceField(), row, mapping.getDefaultValue());
                break;
            case "composite":
                // Ensure 'sources', 'transform', 'delimiter', and 'defaultValue' are available in mapping
                value = handleComposite(
                    Optional.ofNullable(mapping.getSources()).orElse(Collections.emptyList()), 
                    row, 
                    mapping.getTransform(), 
                    mapping.getDelimiter(), 
                    mapping.getDefaultValue()
                );
                break;
            case "conditional":
                // Ensure 'conditions' and 'defaultValue' are available in mapping
                value = evaluateConditional(
                    Optional.ofNullable(mapping.getConditions()).orElse(Collections.emptyList()), 
                    row, 
                    mapping.getDefaultValue()
                );
                break;
            default:
                value = mapping.getDefaultValue(); // Fallback to default value if transformation type is unknown or not set
        }

        // Apply padding
        return FormatterUtil.pad(value, mapping.getLength(), mapping.getPad(), mapping.getPadChar());
    }

    /**
     * Resolves a single value from the input data row using the source field name.
     * If the source field is not found in the row or its value is null, the provided default value is returned.
     *
     * @param sourceField The name of the field in the input row to retrieve the value from.
     * @param row The input data row.
     * @param defaultValue The value to return if the source field is not present or is null.
     * @return The resolved value as a String, or the defaultValue.
     */
    private String resolveValue(String sourceField, Map<String, Object> row, String defaultValue) {
        if (sourceField == null || sourceField.isEmpty()) {
            return defaultValue; // No source field specified, return default
        }
        Object value = row.get(sourceField);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Handles composite field transformation, such as concatenation or summation,
     * based on multiple source fields defined in the YAML.
     *
     * @param sources List of source field definitions for the composite operation. Each map in the list should define "sourceField".
     * @param row The input data row.
     * @param transform The type of composite transformation (e.g., "concat", "sum").
     * @param delimiter The delimiter to use for "concat" operations.
     * @param defaultValue The default value if the composite operation cannot be performed or results in null/empty.
     * @return The result of the composite transformation as a String.
     */
    private String handleComposite(List<Map<String, String>> sources, Map<String, Object> row, String transform, String delimiter, String defaultValue) {
        if (sources.isEmpty()) {
            return defaultValue;
        }

        if ("sum".equalsIgnoreCase(transform)) {
            double sum = sources.stream()
                .mapToDouble(s -> {
                    String fieldName = s.get("sourceField");
                    if (fieldName == null) return 0.0;
                    Object val = row.get(fieldName.trim());
                    try {
                        return val != null ? Double.parseDouble(val.toString()) : 0.0;
                    } catch (NumberFormatException e) {
                        // Consider logging this error for diagnostics
                        return 0.0; // Treat non-numeric values as 0 for sum
                    }
                }).sum();
            return String.valueOf(sum); // Consider formatting this output (e.g., number of decimal places)
        } else if ("concat".equalsIgnoreCase(transform)) {
            String actualDelimiter = (delimiter != null) ? delimiter : "";
            return sources.stream()
                    .map(s -> {
                        String fieldName = s.get("sourceField");
                        if (fieldName == null) return ""; // Or handle as error/default part
                        return Optional.ofNullable(row.get(fieldName.trim())).map(Object::toString).orElse("");
                    })
                    .collect(Collectors.joining(actualDelimiter));
        }
        return defaultValue; // Fallback if transform type is not recognized
    }

    /**
     * Evaluates conditional transformation logic for a field.
     * The method processes a list of conditions. It's assumed that CsvToYamlConverter
     * (or manual YAML configuration) provides a list containing one primary {@link com.truist.batch.model.Condition} object
     * for a given FieldMapping's 'conditions' block.
     * This primary Condition object then holds:
     *  - The main 'if' expression and its 'then' value (field name or literal).
     *  - A list of 'else if' conditions (each with its own expression and 'then' value).
     *  - An 'else' value (field name or literal).
     *
     * The evaluation order is:
     * 1. The main 'if' expression.
     * 2. If the main 'if' is false, each 'else if' expression is evaluated in the order they are defined.
     * 3. If all 'if' and 'else if' expressions are false, the 'else' value is used.
     * 4. If no conditions were met (e.g., no 'if' was true, no 'else if' was true, and no 'else' was defined or matched), 
     *    the overall 'defaultValue' for the FieldMapping is returned.
     *
     * Values for 'then', 'else if then', and 'else' can be literals or names of other source fields to resolve.
     *
     * @param conditions List of conditions to evaluate (typically expected to contain one main Condition object).
     * @param row The input data row.
     * @param defaultValue The default value for the field if no conditions are met or evaluate to true.
     * @return The resolved value based on the conditional logic.
     */
    private String evaluateConditional(List<com.truist.batch.model.Condition> conditions, Map<String, Object> row, String defaultValue) {
        if (conditions == null || conditions.isEmpty()) {
            return defaultValue != null ? defaultValue : "";
        }

        // Process the first main Condition object in the list.
        // CsvToYamlConverter creates a singletonList for field.setConditions().
        com.truist.batch.model.Condition mainCondition = conditions.get(0);

        String ifExpr = mainCondition.getIfExpr();
        String thenVal = mainCondition.getThen(); 
        String elseVal = mainCondition.getElseExpr(); 
        List<com.truist.batch.model.Condition> elseIfConditions = mainCondition.getElseIfExprs();

        // 1. Check the main 'if' condition
        if (ifExpr != null && !ifExpr.isEmpty() && evaluateExpression(ifExpr, row)) {
            return resolveValue(thenVal, row, thenVal); 
        }

        // 2. Check 'else if' conditions (if main 'if' was false)
        if (elseIfConditions != null && !elseIfConditions.isEmpty()) {
            for (com.truist.batch.model.Condition elseIfCondition : elseIfConditions) {
                String elseIfExpr = elseIfCondition.getIfExpr();
                // Crucially, assumes 'then' field within each 'elseIf' Condition object stores its result value/field.
                String elseIfThenVal = elseIfCondition.getThen(); 

                if (elseIfExpr != null && !elseIfExpr.isEmpty() && evaluateExpression(elseIfExpr, row)) {
                    return resolveValue(elseIfThenVal, row, elseIfThenVal); 
                }
            }
        }

        // 3. Apply 'else' value (if main 'if' and all 'else if' were false)
        // Ensure elseVal can be a source field name or a literal.
        if (elseVal != null && !elseVal.isEmpty()) { 
            // Check if elseVal is a field name in the row OR if it's a literal.
            // The current resolveValue handles this: if not in row, it returns elseVal itself.
            return resolveValue(elseVal, row, elseVal); 
        }
        
        // 4. Default value if no conditions were met or no 'else' was provided/matched.
        return defaultValue != null ? defaultValue : "";
    }

    /**
     * Evaluates simple logical expressions from the YAML configuration.
     * <p>
     * Currently supports:
     * <ul>
     *     <li>Equality: {@code "fieldName = 'stringValue'"} or {@code "fieldName = stringValue"}.
     *         String values in the expression should be enclosed in single quotes if they contain spaces or special characters,
     *         though quotes are optional for simple alphanumeric strings. Comparison is case-sensitive.</li>
     *     <li>Less than: {@code "fieldName < numericValue"}</li>
     *     <li>Greater than: {@code "fieldName > numericValue"}</li>
     * </ul>
     * Field names are resolved from the input row. Numeric values are parsed as doubles.
     * </p><p>
     * Limitations:
     * <ul>
     *     <li>Does not support combined expressions (AND/OR).</li>
     *     <li>Equality check is always string-based after converting row value to string.
     *         For reliable numeric equality, ensure string formatting is consistent or use range checks if possible.</li>
     *     <li>No support for other operators like '!=', '<=', '>='.</li>
     * </ul>
     * </p>
     * @param expression The conditional expression string (e.g., "STATUS = 'Active'", "AGE < 30").
     * @param row The input data row as a Map.
     * @return {@code true} if the expression evaluates to true, {@code false} otherwise.
     */
    boolean evaluateExpression(String expression, Map<String, Object> row) { 
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        String[] parts;
        String operator = null;

        if (expression.contains("=")) {
            parts = expression.split("=", 2);
            operator = "=";
        } else if (expression.contains("<")) {
            parts = expression.split("<", 2);
            operator = "<";
        } else if (expression.contains(">")) {
            parts = expression.split(">", 2);
            operator = ">";
        } else {
            // Unsupported expression format
            return false;
        }

        if (parts.length < 2) {
            return false; // Malformed expression
        }

        String fieldName = parts[0].trim();
        String valueToCompare = parts[1].trim();
        Object rawValueFromRow = row.get(fieldName);

        if (rawValueFromRow == null) {
            // If field is not in row, it cannot satisfy the condition unless comparing against null (not supported yet)
            return false; 
        }
        String actualValueStr = rawValueFromRow.toString();

        if ("=".equals(operator)) {
            // Strip single quotes if present for comparison, for flexibility
            if (valueToCompare.startsWith("'") && valueToCompare.endsWith("'") && valueToCompare.length() > 1) {
                valueToCompare = valueToCompare.substring(1, valueToCompare.length() - 1);
            }
            return actualValueStr.equals(valueToCompare);
        } else if ("<".equals(operator) || ">".equals(operator)) {
            try {
                double actualNumericValue = Double.parseDouble(actualValueStr);
                double valueToCompareNumeric = Double.parseDouble(valueToCompare);
                if ("<".equals(operator)) {
                    return actualNumericValue < valueToCompareNumeric;
                } else { // ">"
                    return actualNumericValue > valueToCompareNumeric;
                }
            } catch (NumberFormatException e) {
                // Consider logging e for better diagnostics during rule setup.
                return false; // Cannot perform numeric comparison if parsing fails
            }
        }
        return false; // Should not reach here if operator was recognized
    }
}

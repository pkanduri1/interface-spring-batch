package com.truist.batch.mapping;

import java.io.IOException;
import java.io.InputStream;
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

import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;
import com.truist.batch.util.FormatterUtil;

@Service
public class YamlMappingService {

    /**
     * Loads field mappings from a YAML file located at the given path.
     * The mappings are returned as a sorted list of entries based on the target position.
     *
     * @param yamlPath the path to the YAML file in classpath
     * @return a sorted list of field mapping entries
     */
    public List<Map.Entry<String, FieldMapping>> loadFieldMappings(String yamlPath) {
        try (InputStream input = new ClassPathResource(yamlPath).getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yml = new Yaml(new Constructor(YamlMapping.class, options));
            YamlMapping yamlMapping = yml.load(input);
            return yamlMapping.getFields().entrySet().stream().sorted(
                    (Comparator.comparingInt(e -> e.getValue().getTargetPosition()))
            ).toList();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to load YAML fro path: " + yamlPath, ioe);
        }
    }

    /**
     * Applies transformation logic to a single field from a row based on the provided mapping.
     *
     * @param row the input data row
     * @param mapping the field mapping definition
     * @return the transformed value as a string
     */
    public String transformField(Map<String, Object> row, FieldMapping mapping) {
        String value = "";

        switch (Optional.ofNullable(mapping.getTransformationType()).orElse("").toLowerCase()) {
            case "constant":
                value = mapping.getValue();
                break;
            case "source":
                value = resolveValue(mapping.getSourceField(), row, mapping.getDefaultValue());
                break;
            case "composite":
                value = handleComposite(mapping.getSources(), row, mapping.getTransform(), mapping.getDelimiter(), mapping.getDefaultValue());
                break;
            case "conditional":
                value = evaluateConditional(mapping.getConditions(), row, mapping.getDefaultValue());
                break;
            default:
                value = mapping.getDefaultValue();
        }

        return FormatterUtil.pad(value, mapping.getLength(), mapping.getPad(), mapping.getPadChar());
    }

    /**
     * Resolves a single value from the row using the source field name,
     * or returns a default value if not found.
     */
    private String resolveValue(String sourceField, Map<String, Object> row, String defaultValue) {
        Object value = row.get(sourceField);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Handles composite field transformation such as concatenation or summation
     * across multiple source fields.
     */
    private String handleComposite(List<Map<String, String>> sources, Map<String, Object> row, String transform, String delimiter, String defaultValue) {
        if ("sum".equalsIgnoreCase(transform)) {
            double sum = sources.stream().mapToDouble(s -> {
                String field = s.get("sourceField");
                Object val = row.get(field);
                try {
                    return val != null ? Double.parseDouble(val.toString()) : 0.0;
                } catch (Exception e) {
                    return 0.0;
                }
            }).sum();
            return String.valueOf(sum);
        }

        List<String> parts = sources.stream()
                .map(s -> Optional.ofNullable(row.get(s.get("sourceField"))).map(Object::toString).orElse(""))
                .collect(Collectors.toList());

        return "concat".equalsIgnoreCase(transform)
                ? String.join(delimiter != null ? delimiter : "", parts)
                : defaultValue;
    }

    /**
     * Evaluates conditional transformation logic for a field using if/else expressions.
     */
    private String evaluateConditional(List<com.truist.batch.model.Condition> conditions, Map<String, Object> row, String defaultValue) {
        for (com.truist.batch.model.Condition condition : conditions) {
            String ifExpr = condition.getIfExpr();
            String thenVal = condition.getThen();
            String elseVal = condition.getElseExpr();
            List<com.truist.batch.model.Condition> elseIfVal = condition.getElseIfExprs();

            if (ifExpr != null && evaluateExpression(ifExpr, row)) {
                return resolveValue(thenVal, row, thenVal);
            } else if (elseVal != null) {
                return resolveValue(elseVal, row, elseVal);
            }
        }
        return defaultValue != null ? defaultValue : "";
    }

    /**
     * Evaluates simple logical expressions like =, <, or > using the input row values.
     */
    private boolean evaluateExpression(String expression, Map<String, Object> row) {
        if (expression.contains("=")) {
            String[] parts = expression.split("=");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("'", "");
                Object leftVal = row.get(left);
                return leftVal != null && leftVal.toString().equals(right);
            }
        } else if (expression.contains("<")) {
            String[] parts = expression.split("<");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(Optional.ofNullable(row.get(parts[0].trim())).orElse("0").toString());
                    double right = Double.parseDouble(parts[1].trim());
                    return left < right;
                } catch (Exception e) {
                    return false;
                }
            }
        } else if (expression.contains(">")) {
            String[] parts = expression.split(">");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(Optional.ofNullable(row.get(parts[0].trim())).orElse("0").toString());
                    double right = Double.parseDouble(parts[1].trim());
                    return left > right;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }
}

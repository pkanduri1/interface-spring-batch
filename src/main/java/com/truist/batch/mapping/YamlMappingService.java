package com.truist.batch.mapping;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;
import com.truist.batch.util.FormatterUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for loading and caching YAML mappings.
 * Supports multi-document YAML files, providing methods to load mappings,
 * select mappings based on transaction type, and transform fields with
 * padding and formatting according to mapping definitions.
 */
@Slf4j
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
     * Loads all YAML documents for the given template path and returns the one matching the specified transaction type.
     * If no exact match is found, returns the default mapping.
     * Throws a RuntimeException if no suitable mapping is found.
     *
     * @param template the YAML template path
     * @param txnType the transaction type to match (case-insensitive)
     * @return the matching YamlMapping object
     */
    public YamlMapping getMapping(String template, String txnType) {
        List<YamlMapping> all = loadYamlMappings(template);
        return all.stream()
            .filter(m -> txnType != null
                      ? txnType.equalsIgnoreCase(m.getTransactionType())
                      : "default".equalsIgnoreCase(m.getTransactionType()))
            .findFirst()
            .orElseGet(() -> all.stream()
                .filter(m -> "default".equalsIgnoreCase(m.getTransactionType()))
                .findFirst()
                .orElseThrow(() -> 
                    new RuntimeException("No mapping for " + template + "/" + txnType)));
    }
    
    /**
     * Loads multi-document YAML from the specified path.
     * Parses all YAML documents into YamlMapping objects and returns them as a list.
     *
     * @param yamlPath the classpath location of the YAML file
     * @return list of YamlMapping objects representing each document
     */
    public List<YamlMapping> loadYamlMappings(String yamlPath) {
        try (InputStream in = new ClassPathResource(yamlPath).getInputStream()) {
        	LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yml = new Yaml(new Constructor(YamlMapping.class, options));
            List<YamlMapping> docs = new ArrayList<>();
            for (Object o : yml.loadAll(in)) {
                docs.add((YamlMapping) o);
            }
            return docs;
        }
        catch (IOException e) {
            throw new RuntimeException("…", e);
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
                // ✅ FIX: Try getValue() first, then fall back to getDefaultValue()
                value = mapping.getValue();
                if (value == null || value.trim().isEmpty()) {
                    value = mapping.getDefaultValue();
                }
                break;
                
            case "source":
                value = resolveValue(mapping.getSourceField(), row, mapping.getDefaultValue());
                break;
                
            case "composite":
                value = handleComposite(mapping.getSources(),row,mapping.getTransform(),mapping.getDelimiter(),mapping.getDefaultValue()/*... existing params ...*/);
                break;
                
            case "conditional":
                value = evaluateConditional(mapping.getConditions(),row,mapping.getDefaultValue()/*... existing params ...*/);
                break;
                
            case "blank":  // ✅ FIX: Explicit support for "blank"
                value = mapping.getDefaultValue();
                break;
                
            default:
                value = mapping.getDefaultValue();
        }

        // Ensure never null
        if (value == null) {
            value = "";
        }

        return FormatterUtil.pad(value, mapping);
    }

    /**
     * Resolves a single value from the input data row using case-insensitive field lookup.
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
		// ✅ CASE-INSENSITIVE LOOKUP
		Object value = null;
		// First try exact match (fastest)
		if (row.containsKey(sourceField)) {
			value = row.get(sourceField);
		} else {
			// Case-insensitive search
			for (Map.Entry<String, Object> entry : row.entrySet()) {
				if (sourceField.equalsIgnoreCase(entry.getKey())) {
					value = entry.getValue();
					break;
				}
			}
		}
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
     * Evaluates simple logical expressions without external dependencies.
     * Supports operators: =, ==, !=, <, > and logical AND (&&) and OR (||).
     * Variables refer to keys in the row map; literal strings must be quoted.
     *
     * @param expression The conditional expression (e.g. "status != null && balance > 0").
     * @param row A map of variable names to values.
     * @return true if the expression evaluates to true; false otherwise.
     */
    private boolean evaluateExpression(String expression, Map<String, Object> row) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        // Split on OR (||)
        for (String orPart : expression.split("\\|\\|")) {
            boolean andResult = true;
            // Split on AND (&&)
            for (String cond : orPart.split("&&")) {
                cond = cond.trim();
                // --- Unary NOT support
                boolean negation = false;
                if (cond.startsWith("!")) {
                    negation = true;
                    cond = cond.substring(1).trim();
                }
                // Match field operator value, supporting <=, >=, and quoted literals with spaces
                Matcher m = Pattern.compile("([^!=<>()\\s]+)\\s*(==|=|!=|>=|<=|<|>)\\s*('([^']*)'|\"([^\"]*)\"|[^\\s]+)")
                                  .matcher(cond);
                if (!m.matches()) {
                    andResult = false;
                    break;
                }
                String field = m.group(1);
                String op = m.group(2);
                // Extract literal from quotes if present
                String rawVal = m.group(4) != null ? m.group(4)
                              : m.group(5) != null ? m.group(5)
                              : m.group(3);
                String val = rawVal;
                String fieldVal = row.get(field) != null ? row.get(field).toString() : null;
                boolean thisResult;
                switch (op) {
                    case "=":
                    case "==":
                        if ("null".equals(val)) {
                            thisResult = fieldVal == null;
                        } else {
                            thisResult = fieldVal != null && fieldVal.equals(val);
                        }
                        break;
                    case "!=":
                        if ("null".equals(val)) {
                            thisResult = fieldVal != null;
                        } else {
                            thisResult = fieldVal == null || !fieldVal.equals(val);
                        }
                        break;
                    case "<":
                    case ">":
                        thisResult = compareNumeric(fieldVal, val, op);
                        break;
                    case "<=":
                        thisResult = compareNumeric(fieldVal, val, "<=");
                        break;
                    case ">=":
                        thisResult = compareNumeric(fieldVal, val, ">=");
                        break;
                    default:
                        thisResult = false;
                }
                if (negation) {
                    thisResult = !thisResult;
                }
                if (!thisResult) {
                    andResult = false;
                    break;
                }
            }
            if (andResult) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to compare numeric values represented as strings.
     */
    private boolean compareNumeric(String fieldVal, String val, String op) {
        try {
            double fv = fieldVal != null ? Double.parseDouble(fieldVal) : 0;
            double vl = Double.parseDouble(val);
            switch (op) {
                case "<":  return fv < vl;
                case ">":  return fv > vl;
                case "<=": return fv <= vl;
                case ">=": return fv >= vl;
                default:   return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}

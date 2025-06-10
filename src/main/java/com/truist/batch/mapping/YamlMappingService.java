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
     */
    public List<Map.Entry<String, FieldMapping>> loadFieldMappings(String yamlPath) {
    	log.info("Loading field mappings from YAML file at path: {}", yamlPath);
        try (InputStream input = new ClassPathResource(yamlPath).getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yml = new Yaml(new Constructor(YamlMapping.class, options));
            YamlMapping yamlMapping = yml.load(input);
            
            if (yamlMapping == null || yamlMapping.getFields() == null) {
                throw new RuntimeException("YAML file at path: " + yamlPath + " is empty or not structured correctly.");
            }

            return yamlMapping.getFields().entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> e.getValue().getTargetPosition()))
                    .collect(Collectors.toList());
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to load YAML from classpath path: " + yamlPath, ioe);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML from classpath path: " + yamlPath, e);
        }
    }
    
    /**  
     * Loads all YAML documents for the given template path and returns the one matching the specified transaction type.
     */
    public YamlMapping getMapping(String template, String txnType) {
    	log.info("Loading YAML mapping for template: {}, transaction type: {}", template, txnType);
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
            throw new RuntimeException("Failed to load YAML from classpath: " + yamlPath, e);
        }
    }

    /**
     * ✅ FIXED: Applies transformation logic to a single field
     */
    public String transformField(Map<String, Object> row, FieldMapping mapping) {
        String value = "";

        String transformationType = Optional.ofNullable(mapping.getTransformationType()).orElse("").toLowerCase();
        
        switch (transformationType) {
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
                value = handleComposite(mapping.getSources(), row, mapping.getTransform(), 
                                      mapping.getDelimiter(), mapping.getDefaultValue());
                break;
                
            case "conditional":
                value = evaluateConditional(mapping.getConditions(), row, mapping.getDefaultValue());
                break;
                
            case "blank":  // ✅ FIX: Explicit support for "blank"
                value = mapping.getDefaultValue();
                break;
                
            default:
                value = mapping.getDefaultValue();
        }

        // ✅ FIX: Ensure never null and handle empty strings properly
        if (value == null) {
            value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
        }

        // ✅ FIX: Apply padding and formatting only if we have a non-empty value
        if (mapping.getLength() > 0) {
            return FormatterUtil.pad(value, mapping);
        }
        
        return value;
    }

    /**
     * ✅ FIXED: Resolves a single value from the input data row using case-insensitive field lookup.
     */
	private String resolveValue(String sourceField, Map<String, Object> row, String defaultValue) {
		if (sourceField == null || sourceField.isEmpty()) {
			return defaultValue != null ? defaultValue : "";
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
		
		// ✅ FIX: Return actual value or default, not empty string
		return value != null ? value.toString() : (defaultValue != null ? defaultValue : "");
	}

   /**
    * Handles composite field transformation
    */
   private String handleComposite(List<Map<String, String>> sources, Map<String, Object> row, 
                                String transform, String delimiter, String defaultValue) {
       if (sources == null || sources.isEmpty()) {
           return defaultValue != null ? defaultValue : "";
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
                       return 0.0;
                   }
               }).sum();
           return String.valueOf(sum);
       } else if ("concat".equalsIgnoreCase(transform)) {
           String actualDelimiter = (delimiter != null) ? delimiter : "";
           return sources.stream()
                   .map(s -> {
                       String fieldName = s.get("sourceField");
                       if (fieldName == null) return "";
                       return Optional.ofNullable(row.get(fieldName.trim())).map(Object::toString).orElse("");
                   })
                   .collect(Collectors.joining(actualDelimiter));
       }
       return defaultValue != null ? defaultValue : "";
   }

   /**
    * Evaluates conditional transformation logic for a field.
    */
   private String evaluateConditional(List<com.truist.batch.model.Condition> conditions, 
                                    Map<String, Object> row, String defaultValue) {
       if (conditions == null || conditions.isEmpty()) {
           return defaultValue != null ? defaultValue : "";
       }

       com.truist.batch.model.Condition mainCondition = conditions.get(0);
       String ifExpr = mainCondition.getIfExpr();
       String thenVal = mainCondition.getThen(); 
       String elseVal = mainCondition.getElseExpr(); 
       List<com.truist.batch.model.Condition> elseIfConditions = mainCondition.getElseIfExprs();

       // 1. Check the main 'if' condition
       if (ifExpr != null && !ifExpr.isEmpty() && evaluateExpression(ifExpr, row)) {
           return resolveValue(thenVal, row, thenVal); 
       }

       // 2. Check 'else if' conditions
       if (elseIfConditions != null && !elseIfConditions.isEmpty()) {
           for (com.truist.batch.model.Condition elseIfCondition : elseIfConditions) {
               String elseIfExpr = elseIfCondition.getIfExpr();
               String elseIfThenVal = elseIfCondition.getThen(); 

               if (elseIfExpr != null && !elseIfExpr.isEmpty() && evaluateExpression(elseIfExpr, row)) {
                   return resolveValue(elseIfThenVal, row, elseIfThenVal); 
               }
           }
       }

       // 3. Apply 'else' value
       if (elseVal != null && !elseVal.isEmpty()) { 
           return resolveValue(elseVal, row, elseVal); 
       }
       
       // 4. Default value
       return defaultValue != null ? defaultValue : "";
   }

    /**
     * ✅ FIXED: Evaluates simple logical expressions
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
                
                // Handle unary NOT
                boolean negation = false;
                if (cond.startsWith("!")) {
                    negation = true;
                    cond = cond.substring(1).trim();
                }
                
                // Match field operator value
                Matcher m = Pattern.compile("([^!=<>()\\s]+)\\s*(==|=|!=|>=|<=|<|>)\\s*('([^']*)'|\"([^\"]*)\"|[^\\s]+)")
                                  .matcher(cond);
                if (!m.matches()) {
                    andResult = false;
                    break;
                }
                
                String field = m.group(1);
                String op = m.group(2);
                String rawVal = m.group(4) != null ? m.group(4)
                              : m.group(5) != null ? m.group(5)
                              : m.group(3);
                
                String fieldVal = row.get(field) != null ? row.get(field).toString() : null;
                boolean thisResult;
                
                switch (op) {
                    case "=":
                    case "==":
                        if ("null".equals(rawVal)) {
                            thisResult = fieldVal == null;
                        } else {
                            thisResult = fieldVal != null && fieldVal.equals(rawVal);
                        }
                        break;
                    case "!=":
                        if ("null".equals(rawVal)) {
                            thisResult = fieldVal != null;
                        } else {
                            thisResult = fieldVal == null || !fieldVal.equals(rawVal);
                        }
                        break;
                    case "<":
                    case ">":
                    case "<=":
                    case ">=":
                        thisResult = compareNumeric(fieldVal, rawVal, op);
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
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

public class CsvToYamlConverter {

    public static void convert(String csvPath, String yamlOutputPath, String fileType) throws Exception {
        Map<String,FieldMapping> fieldMappings = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");

            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                FieldMapping field = new FieldMapping();

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

                Condition condition = new Condition();
                condition.setIfExpr(tokens[headerMap.get("If Expression")].trim());
                condition.setElseExpr(tokens[headerMap.get("Else")].trim());

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

                // Pad character logic based on pad direction
                if (field.getPadChar() == null || field.getPadChar().isEmpty()) {
                    field.setPadChar("left".equalsIgnoreCase(field.getPad()) ? "0" : " ");
                }

                // If defaultValue is empty string
                if (field.getDefaultValue() == null || field.getDefaultValue().isEmpty()) {
                    field.setDefaultValue(" ");
                }

                fieldMappings.put(field.getFieldName(), field);
            }
        }

        YamlMapping yamlMapping = new YamlMapping();
        yamlMapping.setFileType(fileType);
        yamlMapping.setFields(fieldMappings);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (FileWriter writer = new FileWriter(Paths.get(yamlOutputPath).toFile())) {
            mapper.writeValue(writer, yamlMapping);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java CsvToYamlConverter <input.csv> <output.yml> <fileType>");
            return;
        }
        convert(args[0], args[1], args[2]);
    }
}

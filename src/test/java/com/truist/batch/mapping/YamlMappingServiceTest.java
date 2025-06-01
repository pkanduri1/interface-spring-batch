package com.truist.batch.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.truist.batch.model.Condition;
import com.truist.batch.model.FieldMapping;

// @ExtendWith(MockitoExtension.class) // Uncomment if using Mockito annotations
class YamlMappingServiceTest {

    private YamlMappingService yamlMappingService;

    @BeforeEach
    void setUp() {
        yamlMappingService = new YamlMappingService();
    }

    // Helper method to create a FieldMapping for conditional tests
    private FieldMapping createConditionalFieldMapping(Condition mainCondition, String defaultValue, int length, String padDirection, String padChar) {
        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");
        mapping.setConditions(Collections.singletonList(mainCondition)); // CsvToYamlConverter wraps in a list
        mapping.setDefaultValue(defaultValue);
        mapping.setLength(length); // Example length
        mapping.setPad(padDirection); // e.g., "right"
        mapping.setPadChar(padChar); // e.g., " "
        return mapping;
    }

    // Helper method to create a FieldMapping for conditional tests, no default padding for simpler assertion
    private FieldMapping createConditionalFieldMapping(Condition mainCondition, String defaultValue) {
        return createConditionalFieldMapping(mainCondition, defaultValue, 0, null, null); // Length 0 means no padding via FormatterUtil
    }
    
    // --- Tests for Conditional Logic ---

    @Test
    void testTransformField_Conditional_IfTrue_ThenLiteral() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "A");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active_Literal"); // 'then' is a literal value

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default", 15, "right", " ");
        // Expected: "Active_Literal" padded to 15 chars
        assertEquals("Active_Literal ", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_IfTrue_ThenSourceField() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "A");
        row.put("SOURCE_ACTIVE_VAL", "ResolvedFromSource");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("SOURCE_ACTIVE_VAL"); // 'then' is another field name

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default", 0, null, null); // No padding
        assertEquals("ResolvedFromSource", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_ElseIfTrue_ThenLiteral() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "B");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active");

        Condition elseIf1 = new Condition();
        elseIf1.setIfExpr("STATUS = 'B'");
        elseIf1.setThen("Blocked_Literal"); // 'then' for elseIf is a literal

        List<Condition> elseIfs = new ArrayList<>();
        elseIfs.add(elseIf1);
        mainCondition.setElseIfExprs(elseIfs);

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default", 0, null, null);
        assertEquals("Blocked_Literal", yamlMappingService.transformField(row, mapping));
    }
    
    @Test
    void testTransformField_Conditional_SecondElseIfTrue_ThenSourceField() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "C");
        row.put("SOURCE_PENDING_VAL", "ResolvedPending");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active");

        Condition elseIf1 = new Condition();
        elseIf1.setIfExpr("STATUS = 'B'");
        elseIf1.setThen("Blocked");
        
        Condition elseIf2 = new Condition();
        elseIf2.setIfExpr("STATUS = 'C'");
        elseIf2.setThen("SOURCE_PENDING_VAL"); // 'then' for elseIf is a source field

        List<Condition> elseIfs = new ArrayList<>();
        elseIfs.add(elseIf1);
        elseIfs.add(elseIf2);
        mainCondition.setElseIfExprs(elseIfs);

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default", 0, null, null);
        assertEquals("ResolvedPending", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_ElseTrue_ThenLiteral() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "X");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active");
        mainCondition.setElseExpr("Cancelled_Literal"); // 'else' is a literal

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default", 0, null, null);
        assertEquals("Cancelled_Literal", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_ElseTrue_ThenSourceField() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "X");
        row.put("SOURCE_ELSE_VAL", "ResolvedElse");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active");
        mainCondition.setElseExpr("SOURCE_ELSE_VAL"); // 'else' is a source field

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default", 0, null, null);
        assertEquals("ResolvedElse", yamlMappingService.transformField(row, mapping));
    }
    
    @Test
    void testTransformField_Conditional_NoConditionMet_ReturnsDefaultValue() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "Z");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active");

        Condition elseIf1 = new Condition();
        elseIf1.setIfExpr("STATUS = 'B'");
        elseIf1.setThen("Blocked");
        mainCondition.setElseIfExprs(Collections.singletonList(elseIf1));
        // No Else expression set on mainCondition

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "DefaultValueUsed", 0, null, null);
        assertEquals("DefaultValueUsed", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_IfFalse_NoElseIf_NoElse_ReturnsDefault() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "X");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS = 'A'");
        mainCondition.setThen("Active");
        // No ElseIfs, no Else on mainCondition

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "DefaultXYZ", 0, null, null);
        assertEquals("DefaultXYZ", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_EmptyConditionsList_ReturnsDefault() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", "A");

        FieldMapping mapping = new FieldMapping(); // No conditions added
        mapping.setTransformationType("conditional");
        mapping.setDefaultValue("DefaultForEmpty");
        mapping.setLength(0); 

        assertEquals("DefaultForEmpty", yamlMappingService.transformField(row, mapping));
    }

    // --- Additional Tests ---

    @Test
    void testTransformField_Conditional_NullCheckTrue() {
        Map<String, Object> row = new HashMap<>();
        row.put("STATUS", null);

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("STATUS == null");
        mainCondition.setThen("IsNull");

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Default");
        assertEquals("IsNull", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_Conditional_NumericComparison() {
        Map<String, Object> row = new HashMap<>();
        row.put("AMOUNT", "150");

        Condition mainCondition = new Condition();
        mainCondition.setIfExpr("AMOUNT >= 100");
        mainCondition.setThen("High");

        FieldMapping mapping = createConditionalFieldMapping(mainCondition, "Low");
        assertEquals("High", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_SourceOnly() {
        Map<String, Object> row = new HashMap<>();
        row.put("FIELD1", "XYZ");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("source");
        mapping.setSourceField("FIELD1");
        mapping.setDefaultValue("DefaultVal");
        mapping.setLength(0);
        mapping.setPad(null);
        mapping.setPadChar(null);

        assertEquals("XYZ", yamlMappingService.transformField(row, mapping));
    }

    @Test
    void testTransformField_ConstantOnly() {
        Map<String, Object> row = new HashMap<>();

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("constant");
        mapping.setValue("CONSTVAL");
        mapping.setDefaultValue("DefaultVal");
        mapping.setLength(0);
        mapping.setPad(null);
        mapping.setPadChar(null);

        assertEquals("CONSTVAL", yamlMappingService.transformField(row, mapping));
    }
}

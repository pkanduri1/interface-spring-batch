package com.truist.batch.processor;

import com.truist.batch.exception.ItemProcessingException;
import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.util.ErrorLogger;
import com.truist.batch.util.YamlPropertyLoaderService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class P327ProcessorTest {

    @Mock
    private YamlMappingService mockYamlMappingService;

    // For @InjectMocks to work with constructor injection that also uses @Value,
    // we'd typically need Spring testing support. For plain Mockito, manual instantiation is better.
    private P327Processor p327Processor;

    private final String testSourceSystem = "testSource";
    private final String testJobName = "testJob";

    private MockedStatic<YamlPropertyLoaderService> mockedYamlPropertyLoader;
    private MockedStatic<ErrorLogger> mockedErrorLogger;

    @BeforeEach
    void setUp() {
        // Mock static methods before each test
        mockedYamlPropertyLoader = Mockito.mockStatic(YamlPropertyLoaderService.class);
        mockedErrorLogger = Mockito.mockStatic(ErrorLogger.class);

        // Arrange for constructor dependencies
        List<Map.Entry<String, FieldMapping>> mockMappings = Collections.emptyList(); // Or some default mock
        Properties mockProps = new Properties();
        mockProps.setProperty("batch.skip.skip-log-path", "dummy/log/path.log");

        mockedYamlPropertyLoader.when(() -> YamlPropertyLoaderService.loadProperties(testJobName, testSourceSystem))
                               .thenReturn(mockProps);
        when(mockYamlMappingService.loadFieldMappings(testJobName + "/" + testSourceSystem + "/" + testJobName + ".yml"))
                               .thenReturn(mockMappings);
        
        p327Processor = new P327Processor(mockYamlMappingService, testSourceSystem, testJobName);
    }

    @AfterEach
    void tearDown() {
        // Close static mocks after each test
        mockedYamlPropertyLoader.close();
        mockedErrorLogger.close();
    }

    @Test
    void testConstructor_LoadsMappingsAndProperties() {
        // Assertions are implicitly handled by @BeforeEach setup mocks if using lenient mocking.
        // To be explicit:
        String expectedYmlPath = testJobName + "/" + testSourceSystem + "/" + testJobName + ".yml";
        
        // Verify that during P327Processor construction (in setUp), these were called
        verify(mockYamlMappingService).loadFieldMappings(expectedYmlPath);
        mockedYamlPropertyLoader.verify(() -> YamlPropertyLoaderService.loadProperties(testJobName, testSourceSystem));
        
        // We can also check that the processor is not null, etc.
        assertNotNull(p327Processor);
    }

    @Test
    void testProcess_Success() throws Exception {
        // Arrange
        Map<String, Object> item = new HashMap<>();
        item.put("INPUT_KEY", "inputValue");

        FieldMapping fm1 = new FieldMapping();
        fm1.setFieldName("OUTPUT_KEY1"); 
        // Set other necessary properties on fm1 if transformField depends on them

        List<Map.Entry<String, FieldMapping>> fieldMappings = 
            Collections.singletonList(Map.entry(fm1.getFieldName(), fm1));
        
        // Use reflection to set the private 'fieldMappings' field in p327Processor
        // For example, using org.springframework.test.util.ReflectionTestUtils
        // ReflectionTestUtils.setField(p327Processor, "fieldMappings", fieldMappings);
        // Or, if not using Spring, standard Java reflection:
        java.lang.reflect.Field field = P327Processor.class.getDeclaredField("fieldMappings");
        field.setAccessible(true);
        field.set(p327Processor, fieldMappings);

        when(mockYamlMappingService.transformField(item, fm1)).thenReturn("transformedValue1");

        // Act
        Map<String, String> result = p327Processor.process(item);

        // Assert
        assertNotNull(result);
        assertEquals("transformedValue1", result.get("OUTPUT_KEY1"));
        verify(mockYamlMappingService).transformField(item, fm1);
    }

    @Test
    void testProcess_TransformationError_LogsAndThrowsException() throws Exception {
        // Arrange
        Map<String, Object> item = new HashMap<>();
        item.put("FAIL_KEY", "failValue");

        FieldMapping fmError = new FieldMapping();
        fmError.setFieldName("ERROR_FIELD");

        List<Map.Entry<String, FieldMapping>> fieldMappings = 
            Collections.singletonList(Map.entry(fmError.getFieldName(), fmError));
        
        // Use reflection to set fieldMappings
        java.lang.reflect.Field field = P327Processor.class.getDeclaredField("fieldMappings");
        field.setAccessible(true);
        field.set(p327Processor, fieldMappings);

        String errorMessage = "Transform failed!";
        RuntimeException transformationException = new RuntimeException(errorMessage);
        when(mockYamlMappingService.transformField(item, fmError)).thenThrow(transformationException);

        String expectedLogPath = "dummy/log/path.log"; // From properties mock in setUp

        // Act & Assert
        ItemProcessingException thrown = assertThrows(ItemProcessingException.class, () -> {
            p327Processor.process(item);
        });

        assertEquals("Error processing row: " + item, thrown.getMessage());
        assertSame(transformationException, thrown.getCause());

        // Verify static method ErrorLogger.logError was called
        mockedErrorLogger.verify(() -> ErrorLogger.logError(
            item,
            testSourceSystem,
            testJobName,
            errorMessage,
            expectedLogPath
        ), times(1));
    }
}

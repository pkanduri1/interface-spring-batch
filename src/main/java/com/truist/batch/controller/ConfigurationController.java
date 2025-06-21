package com.truist.batch.controller;

import com.truist.batch.service.ConfigurationService;
import com.truist.batch.model.SourceSystem;
import com.truist.batch.model.JobConfig;
import com.truist.batch.model.SourceField;
import com.truist.batch.model.FieldMappingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ui")
@CrossOrigin(origins = "http://localhost:3000") // Your React dev server
public class ConfigurationController {

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Get all source systems
     * Frontend expects: GET /api/ui/source-systems
     */
    @GetMapping("/source-systems")
    public ResponseEntity<List<SourceSystem>> getSourceSystems() {
        try {
            List<SourceSystem> systems = configurationService.getAllSourceSystems();
            return ResponseEntity.ok(systems);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get jobs for a specific source system
     * Frontend expects: GET /api/ui/source-systems/{id}/jobs
     */
    @GetMapping("/source-systems/{systemId}/jobs")
    public ResponseEntity<List<JobConfig>> getJobs(@PathVariable String systemId) {
        try {
            List<JobConfig> jobs = configurationService.getJobsForSystem(systemId);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get source fields for a system
     * Frontend expects: GET /api/ui/source-systems/{id}/fields
     */
    @GetMapping("/source-systems/{systemId}/fields")
    public ResponseEntity<List<SourceField>> getSourceFields(
            @PathVariable String systemId,
            @RequestParam(required = false) String jobName) {
        try {
            List<SourceField> fields = configurationService.getSourceFields(systemId, jobName);
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get field mappings for a specific job
     * Frontend expects: GET /api/ui/mappings/{system}/{job}
     */
    @GetMapping("/mappings/{sourceSystem}/{jobName}")
    public ResponseEntity<FieldMappingConfig> getMappings(
            @PathVariable String sourceSystem,
            @PathVariable String jobName) {
        try {
            FieldMappingConfig config = configurationService.getFieldMappings(sourceSystem, jobName);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get field mappings for specific transaction type
     * Frontend expects: GET /api/ui/mappings/{system}/{job}/{txnType}
     */
    @GetMapping("/mappings/{sourceSystem}/{jobName}/{transactionType}")
    public ResponseEntity<FieldMappingConfig> getMappingsForTransaction(
            @PathVariable String sourceSystem,
            @PathVariable String jobName,
            @PathVariable String transactionType) {
        try {
            FieldMappingConfig config = configurationService.getFieldMappings(
                sourceSystem, jobName, transactionType);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Save field mapping configuration
     * Frontend expects: POST /api/ui/mappings/save
     */
    @PostMapping("/mappings/save")
    public ResponseEntity<Map<String, Object>> saveConfiguration(
            @RequestBody FieldMappingConfig config) {
        try {
            String configId = configurationService.saveConfiguration(config);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "configId", configId,
                "message", "Configuration saved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Validate field mapping configuration
     * Frontend expects: POST /api/ui/mappings/validate
     */
    @PostMapping("/mappings/validate")
    public ResponseEntity<Map<String, Object>> validateConfiguration(
            @RequestBody FieldMappingConfig config) {
        try {
            var validationResult = configurationService.validateConfiguration(config);
            return ResponseEntity.ok(Map.of(
                "valid", validationResult.isValid(),
                "errors", validationResult.getErrors(),
                "warnings", validationResult.getWarnings()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "errors", List.of(e.getMessage())
            ));
        }
    }

    /**
     * Generate YAML from configuration
     * Frontend expects: POST /api/ui/mappings/generate-yaml
     */
    @PostMapping("/mappings/generate-yaml")
    public ResponseEntity<Map<String, Object>> generateYaml(
            @RequestBody FieldMappingConfig config) {
        try {
            String yaml = configurationService.generateYaml(config);
            return ResponseEntity.ok(Map.of(
                "yaml", yaml,
                "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Preview output format
     * Frontend expects: POST /api/ui/mappings/preview
     */
    @PostMapping("/mappings/preview")
    public ResponseEntity<Map<String, Object>> previewOutput(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            FieldMappingConfig mapping = (FieldMappingConfig) request.get("mapping");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sampleData = (List<Map<String, Object>>) request.get("sampleData");
            
            List<String> preview = configurationService.generatePreview(mapping, sampleData);
            return ResponseEntity.ok(Map.of("preview", preview));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Test configuration with sample data
     * Frontend expects: POST /api/ui/test/{system}/{job}
     */
    @PostMapping("/test/{sourceSystem}/{jobName}")
    public ResponseEntity<Map<String, Object>> testConfiguration(
            @PathVariable String sourceSystem,
            @PathVariable String jobName) {
        try {
            var testResult = configurationService.testConfiguration(sourceSystem, jobName);
            return ResponseEntity.ok(Map.of(
                "success", testResult.isSuccess(),
                "message", testResult.getMessage(),
                "sampleOutput", testResult.getSampleOutput(),
                "executionTime", testResult.getExecutionTimeMs()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
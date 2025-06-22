package com.truist.batch.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateImportResult {
    private boolean success;
    private String fileType;
    private Integer fieldsImported;
    private Integer fieldsSkipped;
    private List<String> errors;
    private List<String> warnings;
    private String message;
}

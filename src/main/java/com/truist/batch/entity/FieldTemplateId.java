package com.truist.batch.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldTemplateId implements Serializable {
    private static final long serialVersionUID = 1L;
	private String fileType;
    private String transactionType;
    private String fieldName;
}

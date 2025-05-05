package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Common Word entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonWordDTO {
    private Long id;
    private String sourceText;
    private String targetText;
    private String sourceLanguageCode;
    private String targetLanguageCode;
    private Integer usageCount;
} 
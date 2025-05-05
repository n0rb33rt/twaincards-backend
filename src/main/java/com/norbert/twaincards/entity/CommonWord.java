package com.norbert.twaincards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "common_words",
       indexes = {
           @Index(name = "idx_source_text", columnList = "source_text"),
           @Index(name = "idx_source_language_target_language", columnList = "source_language_code, target_language_code")
       }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_text", nullable = false, length = 255)
    private String sourceText;
    
    @Column(name = "target_text", nullable = false, length = 255)
    private String targetText;
    
    @Column(name = "source_language_code", nullable = false, length = 10)
    private String sourceLanguageCode;
    
    @Column(name = "target_language_code", nullable = false, length = 10)
    private String targetLanguageCode;
} 
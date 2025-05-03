package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO для передачі даних колекції
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDTO {

  private Long id;

  private Long userId;
  private String username;

  @NotBlank(message = "Назва колекції не може бути пустою")
  @Size(min = 2, max = 100, message = "Назва колекції повинна містити від 2 до 100 символів")
  private String name;

  @Size(max = 500, message = "Опис колекції повинен містити не більше 500 символів")
  private String description;

  @NotNull(message = "Мова оригіналу не може бути пустою")
  private Long sourceLanguageId;
  private String sourceLanguageName;
  private String sourceLanguageCode;

  @NotNull(message = "Мова перекладу не може бути пустою")
  private Long targetLanguageId;
  private String targetLanguageName;
  private String targetLanguageCode;

  private Boolean isPublic;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Додаткова інформація
  private Integer cardCount;
  private Integer learnedCardCount;
  private Double completionPercentage;
}
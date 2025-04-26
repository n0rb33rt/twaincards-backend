package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO для передачі даних тегу
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {

  private Long id;

  @NotBlank(message = "Назва тегу не може бути пустою")
  @Size(min = 1, max = 50, message = "Назва тегу повинна містити від 1 до 50 символів")
  private String name;

  private LocalDateTime createdAt;

  // Додаткова інформація
  private Integer cardCount;
}
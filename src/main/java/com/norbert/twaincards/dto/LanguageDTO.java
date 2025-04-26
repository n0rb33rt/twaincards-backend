package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO для передачі даних мови
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDTO {

  private Long id;

  @NotBlank(message = "Код мови не може бути пустим")
  @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$", message = "Код мови повинен відповідати формату ISO 639-1 або ISO 639-2")
  private String code;

  @NotBlank(message = "Назва мови не може бути пустою")
  @Size(min = 2, max = 50, message = "Назва мови повинна містити від 2 до 50 символів")
  private String name;

  @Size(max = 50, message = "Рідна назва мови повинна містити не більше 50 символів")
  private String nativeName;

  private Boolean isEnabled;
}
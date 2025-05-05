package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO для передачі даних картки
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {

  private Long id;

  @NotNull(message = "Ідентифікатор колекції не може бути пустим")
  private Long collectionId;

  @NotBlank(message = "Текст на лицьовій стороні не може бути пустим")
  @Size(max = 255, message = "Текст на лицьовій стороні повинен містити не більше 255 символів")
  private String frontText;

  @NotBlank(message = "Текст на зворотній стороні не може бути пустим")
  @Size(max = 255, message = "Текст на зворотній стороні повинен містити не більше 255 символів")
  private String backText;

  @Size(max = 255, message = "Фонетичний текст повинен містити не більше 255 символів")
  private String phoneticText;

  @Size(max = 1000, message = "Приклад використання повинен містити не більше 1000 символів")
  private String exampleUsage;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private String learningStatus;
  private Integer repetitionCount;
  private Integer correctAnswers;
  private Integer incorrectAnswers;
  private LocalDateTime nextReviewDate;


  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateCardRequest {

    @NotNull(message = "Ідентифікатор колекції не може бути пустим")
    private Long collectionId;

    @NotBlank(message = "Текст на лицьовій стороні не може бути пустим")
    @Size(max = 255, message = "Текст на лицьовій стороні повинен містити не більше 255 символів")
    private String frontText;

    @NotBlank(message = "Текст на зворотній стороні не може бути пустим")
    @Size(max = 255, message = "Текст на зворотній стороні повинен містити не більше 255 символів")
    private String backText;

    @Size(max = 255, message = "Фонетичний текст повинен містити не більше 255 символів")
    private String phoneticText;

    @Size(max = 1000, message = "Приклад використання повинен містити не більше 1000 символів")
    private String exampleUsage;

    @Builder.Default
    private Set<String> tagNames = new HashSet<>();
  }

  /**
   * DTO для відповіді на картку
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CardAnswerRequest {

    @NotNull(message = "Ідентифікатор картки не може бути пустим")
    private Long cardId;

    @NotNull(message = "Вказівка правильності відповіді не може бути пустою")
    private Boolean isCorrect;
    
    // Study session ID to associate this answer with a specific study session
    private Long sessionId;
    
    // Response time in milliseconds (optional)
    private Long responseTimeMs;
  }
}
package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.LearningProgressDTO;
import com.norbert.twaincards.service.LearningService;
import com.norbert.twaincards.service.UserActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контролер для управління процесом вивчення
 */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@Slf4j
public class LearningController {

  private final LearningService learningService;
  private final UserActivityLogService activityLogService;

  /**
   * Отримати картки для вивчення з колекції
   */
  @GetMapping("/cards-to-learn")
  public ResponseEntity<List<CardDTO>> getCardsToLearn(
          @RequestAttribute("userId") Long userId,
          @RequestParam Long collectionId,
          @RequestParam(defaultValue = "10") int limit) {

    log.info("Request to get cards to learn for collection with id: {}, limit: {}", collectionId, limit);
    return ResponseEntity.ok(learningService.getCardsToLearn(userId, collectionId, limit));
  }

  /**
   * Отримати картки для повторення
   */
  @GetMapping("/cards-for-review")
  public ResponseEntity<List<LearningProgressDTO>> getCardsForReview(
          @RequestAttribute("userId") Long userId,
          @RequestParam(defaultValue = "10") int limit) {

    log.info("Request to get cards for review with limit: {}", limit);
    return ResponseEntity.ok(learningService.getCardsForReview(userId, limit));
  }

  /**
   * Отримати картки для повторення з колекції
   */
  @GetMapping("/cards-for-review/collection/{collectionId}")
  public ResponseEntity<List<LearningProgressDTO>> getCardsForReviewByCollection(
          @RequestAttribute("userId") Long userId,
          @PathVariable Long collectionId,
          @RequestParam(defaultValue = "10") int limit) {

    log.info("Request to get cards for review from collection with id: {}, limit: {}", collectionId, limit);
    return ResponseEntity.ok(learningService.getCardsForReviewByCollection(userId, collectionId, limit));
  }

  /**
   * Отримати прогрес вивчення для картки
   */
  @GetMapping("/progress/card/{cardId}")
  public ResponseEntity<LearningProgressDTO> getProgressForCard(
          @RequestAttribute("userId") Long userId,
          @PathVariable Long cardId) {

    log.info("Request to get learning progress for card with id: {}", cardId);
    return ResponseEntity.ok(learningService.getProgressForCard(userId, cardId));
  }

  /**
   * Отримати прогрес вивчення для користувача
   */
  @GetMapping("/progress")
  public ResponseEntity<Page<LearningProgressDTO>> getUserProgress(
          @RequestAttribute("userId") Long userId,
          Pageable pageable) {

    log.info("Request to get learning progress page for user");
    return ResponseEntity.ok(learningService.getUserProgress(userId, pageable));
  }

  /**
   * Відповідь на картку (оновлення прогресу вивчення)
   */
  @PostMapping("/answer")
  public ResponseEntity<LearningProgressDTO> answerCard(
          @RequestAttribute("userId") Long userId,
          @RequestBody @Valid CardDTO.CardAnswerRequest cardAnswerRequest) {

    log.info("Request to answer card with id: {}", cardAnswerRequest.getCardId());
    LearningProgressDTO progressDTO = learningService.answerCard(userId, cardAnswerRequest);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "ANSWER_CARD",
            "CARD",
            cardAnswerRequest.getCardId(),
            "Card answered, correct: " + cardAnswerRequest.getIsCorrect()
    );

    return ResponseEntity.ok(progressDTO);
  }

  /**
   * Скинути прогрес вивчення для картки
   */
  @PostMapping("/reset-progress/card/{cardId}")
  public ResponseEntity<Void> resetCardProgress(
          @RequestAttribute("userId") Long userId,
          @PathVariable Long cardId) {

    log.info("Request to reset progress for card with id: {}", cardId);
    learningService.resetCardProgress(userId, cardId);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "RESET_PROGRESS",
            "CARD",
            cardId,
            "Card progress reset"
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Скинути прогрес вивчення для всіх карток колекції
   */
  @PostMapping("/reset-progress/collection/{collectionId}")
  public ResponseEntity<Void> resetCollectionProgress(
          @RequestAttribute("userId") Long userId,
          @PathVariable Long collectionId) {

    log.info("Request to reset progress for collection with id: {}", collectionId);
    learningService.resetCollectionProgress(userId, collectionId);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "RESET_PROGRESS",
            "COLLECTION",
            collectionId,
            "Collection progress reset"
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Отримати статистику прогресу за статусами
   */
  @GetMapping("/status-statistics")
  public ResponseEntity<List<LearningProgressDTO.StatusStatisticsDTO>> getStatusStatistics(
          @RequestAttribute("userId") Long userId) {

    log.info("Request to get status statistics");
    return ResponseEntity.ok(learningService.getStatusStatistics(userId));
  }

  /**
   * Отримати статистику прогресу за статусами для колекції
   */
  @GetMapping("/status-statistics/collection/{collectionId}")
  public ResponseEntity<List<LearningProgressDTO.StatusStatisticsDTO>> getStatusStatisticsForCollection(
          @RequestAttribute("userId") Long userId,
          @PathVariable Long collectionId) {

    log.info("Request to get status statistics for collection with id: {}", collectionId);
    return ResponseEntity.ok(learningService.getStatusStatisticsForCollection(userId, collectionId));
  }
}
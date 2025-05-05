package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.LearningProgressDTO;
import com.norbert.twaincards.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@Slf4j
public class LearningController {

  private final LearningService learningService;

  @GetMapping("/cards-to-learn")
  public ResponseEntity<List<CardDTO>> getCardsToLearn(
          @RequestParam Long collectionId,
          @RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get cards to learn for collection with id: {}, limit: {}", collectionId, limit);
    return ResponseEntity.ok(learningService.getCardsToLearn(collectionId, limit));
  }

  @GetMapping("/cards-for-review")
  public ResponseEntity<List<LearningProgressDTO>> getCardsForReview() {
    return ResponseEntity.ok(learningService.getCardsForReview());
  }

  @GetMapping("/cards-for-review/collection/{collectionId}")
  public ResponseEntity<List<LearningProgressDTO>> getCardsForReviewByCollection(
          @PathVariable Long collectionId,
          @RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get cards for review from collection with id: {}, limit: {}", collectionId, limit);
    return ResponseEntity.ok(learningService.getCardsForReviewByCollection(collectionId, limit));
  }

  @GetMapping("/progress/card/{cardId}")
  public ResponseEntity<LearningProgressDTO> getProgressForCard(
          @PathVariable Long cardId) {
    log.info("Request to get learning progress for card with id: {}", cardId);
    return ResponseEntity.ok(learningService.getProgressForCard(cardId));
  }

  @GetMapping("/progress")
  public ResponseEntity<Page<LearningProgressDTO>> getUserProgress(
          Pageable pageable) {
    log.info("Request to get learning progress page for user");
    return ResponseEntity.ok(learningService.getUserProgress(pageable));
  }

  @PostMapping("/answer")
  public ResponseEntity<LearningProgressDTO> answerCard(
          @RequestBody @Valid CardDTO.CardAnswerRequest cardAnswerRequest) {
    log.info("Request to answer card with id: {}", cardAnswerRequest.getCardId());
    LearningProgressDTO progressDTO = learningService.answerCard(cardAnswerRequest);
    return ResponseEntity.ok(progressDTO);
  }

  @PostMapping("/reset-progress/card/{cardId}")
  public ResponseEntity<Void> resetCardProgress(
          @PathVariable Long cardId) {
    log.info("Request to reset progress for card with id: {}", cardId);
    learningService.resetCardProgress(cardId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-progress/collection/{collectionId}")
  public ResponseEntity<Void> resetCollectionProgress(
          @PathVariable Long collectionId) {
    log.info("Request to reset progress for collection with id: {}", collectionId);
    learningService.resetCollectionProgress(collectionId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/status-statistics")
  public ResponseEntity<List<LearningProgressDTO.StatusStatisticsDTO>> getStatusStatistics() {
    log.info("Request to get status statistics");
    return ResponseEntity.ok(learningService.getStatusStatistics());
  }

  @GetMapping("/status-statistics/collection/{collectionId}")
  public ResponseEntity<List<LearningProgressDTO.StatusStatisticsDTO>> getStatusStatisticsForCollection(
          @PathVariable Long collectionId) {
    log.info("Request to get status statistics for collection with id: {}", collectionId);
    return ResponseEntity.ok(learningService.getStatusStatisticsForCollection(collectionId));
  }
}
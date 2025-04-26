package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.LearningHistoryDTO;
import com.norbert.twaincards.dto.UserStatisticsDTO;
import com.norbert.twaincards.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контролер для отримання статистики
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

  private final StatisticsService statisticsService;

  /**
   * Отримати статистику користувача
   */
  @GetMapping("/user")
  public ResponseEntity<UserStatisticsDTO> getUserStatistics(@RequestAttribute("userId") Long userId) {
    log.info("Request to get statistics for user");
    return ResponseEntity.ok(statisticsService.getUserStatistics(userId));
  }

  /**
   * Отримати статистику за мовами для користувача
   */
  @GetMapping("/languages")
  public ResponseEntity<List<UserStatisticsDTO.LanguageStatisticsDTO>> getLanguageStatistics(
          @RequestAttribute("userId") Long userId) {

    log.info("Request to get language statistics for user");
    return ResponseEntity.ok(statisticsService.getLanguageStatistics(userId));
  }

  /**
   * Отримати статистику активності користувача
   */
  @GetMapping("/activity")
  public ResponseEntity<UserStatisticsDTO.ActivityStatisticsDTO> getActivityStatistics(
          @RequestAttribute("userId") Long userId,
          @RequestParam(defaultValue = "30") int days) {

    log.info("Request to get activity statistics for user for the last {} days", days);
    return ResponseEntity.ok(statisticsService.getActivityStatistics(userId, days));
  }

  /**
   * Отримати статистику прогресу для колекції
   */
  @GetMapping("/collection/{collectionId}")
  public ResponseEntity<UserStatisticsDTO> getCollectionStatistics(
          @RequestAttribute("userId") Long userId,
          @PathVariable Long collectionId) {

    log.info("Request to get collection statistics for collection with id: {}", collectionId);
    return ResponseEntity.ok(statisticsService.getCollectionStatistics(userId, collectionId));
  }

  /**
   * Отримати статистику за останній період
   */
  @GetMapping("/summary")
  public ResponseEntity<LearningHistoryDTO.SummaryStatisticsDTO> getSummaryStatistics(
          @RequestAttribute("userId") Long userId,
          @RequestParam(defaultValue = "30") int days) {

    log.info("Request to get summary statistics for user for the last {} days", days);
    return ResponseEntity.ok(statisticsService.getSummaryStatistics(userId, days));
  }

  /**
   * Отримати статистику найкращих користувачів
   */
  @GetMapping("/top-users/learned-cards")
  public ResponseEntity<List<UserStatisticsDTO>> getTopUsersByLearnedCards(
          @RequestParam(defaultValue = "10") int limit) {

    log.info("Request to get top {} users by learned cards", limit);
    return ResponseEntity.ok(statisticsService.getTopUsersByLearnedCards(limit));
  }

  /**
   * Отримати статистику найкращих користувачів за безперервним навчанням
   */
  @GetMapping("/top-users/learning-streak")
  public ResponseEntity<List<UserStatisticsDTO>> getTopUsersByLearningStreak(
          @RequestParam(defaultValue = "10") int limit) {

    log.info("Request to get top {} users by learning streak", limit);
    return ResponseEntity.ok(statisticsService.getTopUsersByLearningStreak(limit));
  }

  /**
   * Отримати глобальну статистику системи
   */
  @GetMapping("/global")
  public ResponseEntity<UserStatisticsDTO.GlobalStatisticsDTO> getGlobalStatistics() {
    log.info("Request to get global statistics");
    return ResponseEntity.ok(statisticsService.getGlobalStatistics());
  }

  /**
   * Оновити статистику всіх користувачів
   * Додатковий endpoint для оновлення статистики вручну (для адміністраторів)
   */
  @PostMapping("/update-all")
  public ResponseEntity<Void> updateAllUserStatistics() {
    log.info("Request to update statistics for all users");
    statisticsService.updateAllUserStatistics();
    return ResponseEntity.ok().build();
  }
}
package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.LearningHistoryDTO;
import com.norbert.twaincards.dto.UserStatisticsDTO;
import com.norbert.twaincards.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

  private final StatisticsService statisticsService;

  @GetMapping("/user")
  public ResponseEntity<UserStatisticsDTO> getUserStatistics() {
    log.info("Request to get statistics for user");
    return ResponseEntity.ok(statisticsService.getUserStatistics());
  }

  @GetMapping("/activity")
  public ResponseEntity<UserStatisticsDTO.ActivityStatisticsDTO> getActivityStatistics(
          @RequestParam(defaultValue = "30") int days) {
    log.info("Request to get activity statistics for user for the last {} days", days);
    return ResponseEntity.ok(statisticsService.getActivityStatistics(days));
  }

  @GetMapping("/collection/{collectionId}")
  public ResponseEntity<UserStatisticsDTO> getCollectionStatistics(
          @PathVariable Long collectionId) {
    log.info("Request to get collection statistics for collection with id: {}", collectionId);
    return ResponseEntity.ok(statisticsService.getCollectionStatistics(collectionId));
  }

  @GetMapping("/summary")
  public ResponseEntity<LearningHistoryDTO.SummaryStatisticsDTO> getSummaryStatistics(
          @RequestParam(defaultValue = "30") int days) {
    log.info("Request to get summary statistics for user for the last {} days", days);
    return ResponseEntity.ok(statisticsService.getSummaryStatistics(days));
  }

  @GetMapping("/top-users/learned-cards")
  public ResponseEntity<List<UserStatisticsDTO>> getTopUsersByLearnedCards(
          @RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get top {} users by learned cards", limit);
    return ResponseEntity.ok(statisticsService.getTopUsersByLearnedCards(limit));
  }
}
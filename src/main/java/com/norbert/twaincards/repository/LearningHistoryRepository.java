package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Card;
import com.norbert.twaincards.entity.LearningHistory;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.enumeration.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторій для роботи з історією вивчення
 */
@Repository
public interface LearningHistoryRepository extends JpaRepository<LearningHistory, Long> {

  /**
   * Отримання історії вивчення користувача
   * @param user користувач
   * @return список записів історії
   */
  List<LearningHistory> findByUser(User user);

  /**
   * Отримання історії вивчення користувача з пагінацією
   * @param user користувач
   * @param pageable параметри пагінації
   * @return сторінка записів історії
   */
  Page<LearningHistory> findByUser(User user, Pageable pageable);

  /**
   * Отримання історії вивчення картки
   * @param card картка
   * @return список записів історії
   */
  List<LearningHistory> findByCard(Card card);

  /**
   * Отримання історії вивчення картки для користувача
   * @param user користувач
   * @param card картка
   * @return список записів історії
   */
  List<LearningHistory> findByUserAndCard(User user, Card card);

  /**
   * Отримання історії вивчення користувача за типом дії
   * @param user користувач
   * @param actionType тип дії
   * @return список записів історії
   */
  List<LearningHistory> findByUserAndActionType(User user, ActionType actionType);

  /**
   * Отримання історії вивчення користувача за період
   * @param user користувач
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @return список записів історії
   */
  List<LearningHistory> findByUserAndPerformedAtBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Отримання останніх записів історії користувача
   * @param user користувач
   * @param pageable параметри пагінації
   * @return сторінка записів історії
   */
  Page<LearningHistory> findByUserOrderByPerformedAtDesc(User user, Pageable pageable);

  /**
   * Отримання історії вивчення користувача для колекції
   * @param user користувач
   * @param collectionId ідентифікатор колекції
   * @param pageable параметри пагінації
   * @return сторінка записів історії
   */
  @Query("SELECT lh FROM LearningHistory lh JOIN lh.card c WHERE lh.user = :user AND c.collection.id = :collectionId ORDER BY lh.performedAt DESC")
  Page<LearningHistory> findByUserAndCollectionId(
          @Param("user") User user,
          @Param("collectionId") Long collectionId,
          Pageable pageable);

  /**
   * Отримання статистики правильних відповідей користувача за період
   * @param user користувач
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @return масив об'єктів [дата, кількість правильних, кількість неправильних]
   */
  @Query("SELECT FUNCTION('DATE', lh.performedAt) as date, " +
          "SUM(CASE WHEN lh.isCorrect = true THEN 1 ELSE 0 END) as correct, " +
          "SUM(CASE WHEN lh.isCorrect = false THEN 1 ELSE 0 END) as incorrect " +
          "FROM LearningHistory lh " +
          "WHERE lh.user = :user AND lh.actionType = 'REVIEW' " +
          "AND lh.performedAt BETWEEN :startDate AND :endDate " +
          "GROUP BY FUNCTION('DATE', lh.performedAt) " +
          "ORDER BY date")
  List<Object[]> getReviewStatistics(
          @Param("user") User user,
          @Param("startDate") LocalDateTime startDate,
          @Param("endDate") LocalDateTime endDate);


  Long countByUserIdAndPerformedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);


  long countByStudySessionIdAndActionType(Long sessionId, ActionType actionType);

  long countByStudySessionIdAndActionTypeAndIsCorrect(Long sessionId, ActionType actionType, Boolean isCorrect);

  /**
   * Видалення всіх записів історії для картки
   * @param card картка
   */
  void deleteByCard(Card card);
}
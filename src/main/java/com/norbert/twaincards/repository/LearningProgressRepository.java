package com.norbert.twaincards.repository;


import com.norbert.twaincards.entity.Card;
import com.norbert.twaincards.entity.LearningProgress;
import com.norbert.twaincards.entity.LearningProgress.LearningStatus;
import com.norbert.twaincards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторій для роботи з прогресом вивчення карток
 */
@Repository
public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {

  /**
   * Пошук прогресу за користувачем та карткою
   * @param user користувач
   * @param card картка
   * @return опціональний об'єкт прогресу
   */
  Optional<LearningProgress> findByUserAndCard(User user, Card card);

  /**
   * Отримання списку прогресу карток користувача
   * @param user користувач
   * @return список прогресу карток
   */
  List<LearningProgress> findByUser(User user);

  /**
   * Отримання сторінки прогресу карток користувача
   * @param user користувач
   * @param pageable параметри пагінації
   * @return сторінка прогресу карток
   */
  Page<LearningProgress> findByUser(User user, Pageable pageable);

  /**
   * Отримання списку прогресу карток користувача для вказаної колекції
   * @param user користувач
   * @param collectionId ідентифікатор колекції
   * @return список прогресу карток
   */
  @Query("SELECT lp FROM LearningProgress lp JOIN lp.card c WHERE lp.user = :user AND c.collection.id = :collectionId")
  List<LearningProgress> findByUserAndCollectionId(@Param("user") User user, @Param("collectionId") Long collectionId);

  /**
   * Отримання кількості карток, які користувач вже знає
   * @param user користувач
   * @return кількість вивчених карток
   */
  @Query("SELECT COUNT(lp) FROM LearningProgress lp WHERE lp.user = :user AND lp.learningStatus = 'KNOWN'")
  Long countKnownCardsByUser(@Param("user") User user);

  /**
   * Отримання кількості карток, які користувач вивчає
   * @param user користувач
   * @return кількість карток, які вивчаються
   */
  @Query("SELECT COUNT(lp) FROM LearningProgress lp WHERE lp.user = :user AND lp.learningStatus = 'LEARNING'")
  Long countLearningCardsByUser(@Param("user") User user);

  /**
   * Отримання кількості карток, які користувач повторює
   * @param user користувач
   * @return кількість карток на повторенні
   */
  @Query("SELECT COUNT(lp) FROM LearningProgress lp WHERE lp.user = :user AND lp.learningStatus = 'REVIEW'")
  Long countReviewCardsByUser(@Param("user") User user);

  /**
   * Отримання списку карток для повторення
   * @param user користувач
   * @param now поточний час
   * @return список прогресу карток для повторення
   */
  @Query("SELECT lp FROM LearningProgress lp WHERE lp.user = :user AND lp.nextReviewDate <= :now ORDER BY lp.nextReviewDate")
  List<LearningProgress> findCardsForReview(@Param("user") User user, @Param("now") LocalDateTime now);

  /**
   * Отримання списку карток для повторення з вказаної колекції
   * @param user користувач
   * @param collectionId ідентифікатор колекції
   * @param now поточний час
   * @return список прогресу карток для повторення
   */
  @Query("SELECT lp FROM LearningProgress lp JOIN lp.card c WHERE lp.user = :user AND c.collection.id = :collectionId AND lp.nextReviewDate <= :now ORDER BY lp.nextReviewDate")
  List<LearningProgress> findCardsForReviewByCollection(
          @Param("user") User user,
          @Param("collectionId") Long collectionId,
          @Param("now") LocalDateTime now);

  /**
   * Отримання статистики вивчення за статусами
   * @param user користувач
   * @return масив об'єктів [статус, кількість]
   */
  @Query("SELECT lp.learningStatus, COUNT(lp) FROM LearningProgress lp WHERE lp.user = :user GROUP BY lp.learningStatus")
  List<Object[]> getStatusStatisticsByUser(@Param("user") User user);

  /**
   * Отримання статистики вивчення за статусами для колекції
   * @param user користувач
   * @param collectionId ідентифікатор колекції
   * @return масив об'єктів [статус, кількість]
   */
  @Query("SELECT lp.learningStatus, COUNT(lp) FROM LearningProgress lp JOIN lp.card c " +
          "WHERE lp.user = :user AND c.collection.id = :collectionId GROUP BY lp.learningStatus")
  List<Object[]> getStatusStatisticsByUserAndCollection(
          @Param("user") User user,
          @Param("collectionId") Long collectionId);

  /**
   * Отримання карток, які користувач вже знає
   * @param user користувач
   * @param status статус вивчення
   * @param pageable параметри пагінації
   * @return сторінка прогресу карток
   */
  Page<LearningProgress> findByUserAndLearningStatus(User user, LearningStatus status, Pageable pageable);
}
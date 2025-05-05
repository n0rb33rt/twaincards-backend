package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.UserStatistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторій для роботи зі статистикою користувачів
 */
@Repository
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

  /**
   * Пошук статистики за користувачем
   * @param user користувач
   * @return опціональний об'єкт статистики
   */
  Optional<UserStatistics> findByUser(User user);

  /**
   * Отримання статистики з безперервним навчанням більше вказаної кількості днів
   * @param streakDays мінімальна кількість днів
   * @return список статистики користувачів
   */
  List<UserStatistics> findByLearningStreakDaysGreaterThanEqual(Integer streakDays);

  /**
   * Отримання статистики з навчанням у вказаний день
   * @param studyDate дата навчання
   * @return список статистики користувачів
   */
  List<UserStatistics> findByLastStudyDate(LocalDate studyDate);

  /**
   * Отримання статистики для топ користувачів за кількістю вивчених карток
   * @param limit кількість користувачів
   * @return список статистики користувачів
   */
  @Query("SELECT us FROM UserStatistics us ORDER BY us.learnedCards DESC")
  List<UserStatistics> findTopUsersByLearnedCards(Pageable pageable);

  /**
   * Отримання статистики для топ користувачів за тривалістю безперервного навчання
   * @param limit кількість користувачів
   * @return список статистики користувачів
   */
  @Query("SELECT us FROM UserStatistics us ORDER BY us.learningStreakDays DESC")
  List<UserStatistics> findTopUsersByLearningStreak(Pageable pageable);

  /**
   * Отримання загальної статистики навчання
   * @return масив [загальна кількість карток, загальна кількість вивчених карток, загальний час навчання]
   */
  @Query("SELECT SUM(us.totalCards), SUM(us.learnedCards) FROM UserStatistics us")
  Object[] getGlobalStatistics();

  /**
   * Отримання середнього відсотка виконання для всіх користувачів
   * @return середній відсоток
   */
  @Query("SELECT AVG(us.learnedCards * 100.0 / NULLIF(us.totalCards, 0)) FROM UserStatistics us WHERE us.totalCards > 0")
  Double getAverageCompletionPercentage();
}
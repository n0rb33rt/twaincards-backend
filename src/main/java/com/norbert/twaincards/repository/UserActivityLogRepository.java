package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторій для роботи з журналом активності користувачів
 */
@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

  /**
   * Отримання журналу активності користувача
   * @param user користувач
   * @return список записів журналу
   */
  List<UserActivityLog> findByUser(User user);

  /**
   * Отримання журналу активності користувача з пагінацією
   * @param user користувач
   * @param pageable параметри пагінації
   * @return сторінка записів журналу
   */
  Page<UserActivityLog> findByUser(User user, Pageable pageable);

  /**
   * Отримання записів журналу за типом дії
   * @param actionType тип дії
   * @return список записів журналу
   */
  List<UserActivityLog> findByActionType(String actionType);

  /**
   * Отримання записів журналу за типом дії та користувачем
   * @param actionType тип дії
   * @param user користувач
   * @return список записів журналу
   */
  List<UserActivityLog> findByActionTypeAndUser(String actionType, User user);

  /**
   * Отримання записів журналу за типом сутності
   * @param entityType тип сутності
   * @return список записів журналу
   */
  List<UserActivityLog> findByEntityType(String entityType);

  /**
   * Отримання записів журналу за типом сутності та ідентифікатором
   * @param entityType тип сутності
   * @param entityId ідентифікатор сутності
   * @return список записів журналу
   */
  List<UserActivityLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

  /**
   * Отримання записів журналу за період
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @return список записів журналу
   */
  List<UserActivityLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Отримання записів журналу користувача за період
   * @param user користувач
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @return список записів журналу
   */
  List<UserActivityLog> findByUserAndCreatedAtBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Отримання підозрілих дій користувачів
   * @param userIds список ідентифікаторів користувачів
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @param threshold порогове значення кількості дій
   * @return список об'єктів [користувач, тип дії, кількість]
   */
  @Query("SELECT ual.user, ual.actionType, COUNT(ual) as actionCount " +
          "FROM UserActivityLog ual " +
          "WHERE ual.user.id IN :userIds " +
          "AND ual.createdAt BETWEEN :startDate AND :endDate " +
          "GROUP BY ual.user, ual.actionType " +
          "HAVING COUNT(ual) > :threshold " +
          "ORDER BY actionCount DESC")
  List<Object[]> findSuspiciousActivity(
          @Param("userIds") List<Long> userIds,
          @Param("startDate") LocalDateTime startDate,
          @Param("endDate") LocalDateTime endDate,
          @Param("threshold") Long threshold);
}
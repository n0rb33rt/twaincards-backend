package com.norbert.twaincards.service;

import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.UserActivityLog;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.UserActivityLogRepository;
import com.norbert.twaincards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Сервіс для роботи з журналом активності користувачів
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogService {

  private final UserRepository userRepository;
  private final UserActivityLogRepository userActivityLogRepository;

  /**
   * Додавання запису до журналу активності
   * @param userId ідентифікатор користувача
   * @param actionType тип дії
   * @param entityType тип сутності
   * @param entityId ідентифікатор сутності
   * @param description опис дії
   */
  @Transactional
  public void logUserActivity(Long userId, String actionType, String entityType, Long entityId, String description) {
    log.debug("Logging activity for user: {}, action: {}, entity: {}, entityId: {}",
            userId, actionType, entityType, entityId);

    try {
      User user = userRepository.findById(userId).orElse(null);

      // Отримання інформації про запит
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      String ipAddress = null;
      String userAgent = null;

      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        ipAddress = getClientIpAddress(request);
        userAgent = request.getHeader("User-Agent");
      }

      UserActivityLog activityLog = UserActivityLog.builder()
              .user(user)
              .actionType(actionType)
              .entityType(entityType)
              .entityId(entityId)
              .description(description)
              .ipAddress(ipAddress)
              .userAgent(userAgent)
              .createdAt(LocalDateTime.now())
              .build();

      userActivityLogRepository.save(activityLog);
    } catch (Exception e) {
      // Не дозволяємо помилці в логуванні зупинити основний процес
      log.error("Error logging user activity", e);
    }
  }

  /**
   * Отримання активності користувача
   * @param userId ідентифікатор користувача
   * @param pageable параметри пагінації
   * @return сторінка записів журналу активності
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public Page<UserActivityLog> getUserActivity(Long userId, Pageable pageable) {
    log.debug("Getting activity for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    return userActivityLogRepository.findByUser(user, pageable);
  }

  /**
   * Отримання активності користувача за типом дії
   * @param userId ідентифікатор користувача
   * @param actionType тип дії
   * @return список записів журналу активності
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public List<UserActivityLog> getUserActivityByActionType(Long userId, String actionType) {
    log.debug("Getting activity for user with id: {} and action type: {}", userId, actionType);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    return userActivityLogRepository.findByActionTypeAndUser(actionType, user);
  }

  /**
   * Отримання активності за типом сутності та ідентифікатором
   * @param entityType тип сутності
   * @param entityId ідентифікатор сутності
   * @return список записів журналу активності
   */
  @Transactional(readOnly = true)
  public List<UserActivityLog> getActivityByEntityTypeAndId(String entityType, Long entityId) {
    log.debug("Getting activity for entity type: {} and id: {}", entityType, entityId);
    return userActivityLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
  }

  /**
   * Отримання активності за період
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @return список записів журналу активності
   */
  @Transactional(readOnly = true)
  public List<UserActivityLog> getActivityByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Getting activity between: {} and {}", startDate, endDate);
    return userActivityLogRepository.findByCreatedAtBetween(startDate, endDate);
  }

  /**
   * Отримання підозрілої активності користувачів
   * @param userIds список ідентифікаторів користувачів (null для всіх користувачів)
   * @param startDate початкова дата
   * @param endDate кінцева дата
   * @param threshold порогове значення кількості дій
   * @return список об'єктів [користувач, тип дії, кількість]
   */
  @Transactional(readOnly = true)
  public List<Object[]> getSuspiciousActivity(List<Long> userIds, LocalDateTime startDate, LocalDateTime endDate, Long threshold) {
    log.debug("Getting suspicious activity between: {} and {} with threshold: {}", startDate, endDate, threshold);

    // Якщо список користувачів не вказано, використовуємо всіх користувачів
    if (userIds == null || userIds.isEmpty()) {
      userIds = userRepository.findAll().stream()
              .map(User::getId)
              .collect(java.util.stream.Collectors.toList());

      if (userIds.isEmpty()) {
        return Collections.emptyList();
      }
    }

    return userActivityLogRepository.findSuspiciousActivity(userIds, startDate, endDate, threshold);
  }

  /**
   * Отримання IP-адреси клієнта
   * @param request HTTP-запит
   * @return IP-адреса клієнта
   */
  private String getClientIpAddress(HttpServletRequest request) {
    String[] headersToCheck = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : headersToCheck) {
      String ipList = request.getHeader(header);
      if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
        return Arrays.stream(ipList.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .findFirst()
                .orElse(request.getRemoteAddr());
      }
    }

    return request.getRemoteAddr();
  }
}
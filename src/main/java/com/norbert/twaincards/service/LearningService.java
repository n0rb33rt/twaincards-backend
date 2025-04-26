package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.LearningProgressDTO;
import com.norbert.twaincards.entity.*;
import com.norbert.twaincards.entity.LearningHistory.ActionType;
import com.norbert.twaincards.entity.LearningProgress.LearningStatus;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UnauthorizedAccessException;
import com.norbert.twaincards.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервіс для управління процесом вивчення карток
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

  private final CardRepository cardRepository;
  private final UserRepository userRepository;
  private final CollectionRepository collectionRepository;
  private final LearningProgressRepository learningProgressRepository;
  private final LearningHistoryRepository learningHistoryRepository;
  private final UserStatisticsRepository userStatisticsRepository;
  private final ModelMapper modelMapper;

  /**
   * Отримати картки для вивчення з колекції
   * @param userId ідентифікатор користувача
   * @param collectionId ідентифікатор колекції
   * @param limit максимальна кількість карток
   * @return список DTO карток для вивчення
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public List<CardDTO> getCardsToLearn(Long userId, Long collectionId, int limit) {
    log.debug("Getting cards to learn for user: {} from collection: {}", userId, collectionId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Перевірка доступу до колекції
    if (!collection.getIsPublic() && !collection.getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    // Отримуємо картки, для яких ще немає прогресу вивчення
    List<Card> cardsInCollection = cardRepository.findByCollection(collection);
    List<LearningProgress> existingProgress = learningProgressRepository.findByUserAndCollectionId(user, collectionId);

    // Мапа картка -> прогрес вивчення
    Map<Long, LearningProgress> cardProgressMap = existingProgress.stream()
            .collect(Collectors.toMap(progress -> progress.getCard().getId(), progress -> progress));

    // Фільтруємо картки, які ще не почали вивчати або які потрібно повторити
    LocalDateTime now = LocalDateTime.now();

    return cardsInCollection.stream()
            .filter(card -> {
              // Включаємо картки, яких немає в прогресі (нові) або які потрібно повторити
              LearningProgress progress = cardProgressMap.get(card.getId());
              return progress == null ||
                      (progress.getNextReviewDate() != null && !progress.getNextReviewDate().isAfter(now));
            })
            .limit(limit)
            .map(card -> {
              CardDTO cardDTO = modelMapper.map(card, CardDTO.class);
              cardDTO.setCollectionId(card.getCollection().getId());

              // Додаємо інформацію про прогрес вивчення, якщо є
              LearningProgress progress = cardProgressMap.get(card.getId());
              if (progress != null) {
                cardDTO.setLearningStatus(progress.getLearningStatus().name());
                cardDTO.setRepetitionCount(progress.getRepetitionCount());
                cardDTO.setCorrectAnswers(progress.getCorrectAnswers());
                cardDTO.setIncorrectAnswers(progress.getIncorrectAnswers());
                cardDTO.setNextReviewDate(progress.getNextReviewDate());
              } else {
                cardDTO.setLearningStatus(LearningStatus.NEW.name());
                cardDTO.setRepetitionCount(0);
                cardDTO.setCorrectAnswers(0);
                cardDTO.setIncorrectAnswers(0);
              }

              return cardDTO;
            })
            .collect(Collectors.toList());
  }

  /**
   * Отримати картки для повторення
   * @param userId ідентифікатор користувача
   * @param limit максимальна кількість карток
   * @return список DTO прогресу вивчення карток
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public List<LearningProgressDTO> getCardsForReview(Long userId, int limit) {
    log.debug("Getting cards for review for user: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    LocalDateTime now = LocalDateTime.now();
    List<LearningProgress> reviewProgress = learningProgressRepository.findCardsForReview(user, now);

    return reviewProgress.stream()
            .limit(limit)
            .map(this::convertProgressToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати картки для повторення з колекції
   * @param userId ідентифікатор користувача
   * @param collectionId ідентифікатор колекції
   * @param limit максимальна кількість карток
   * @return список DTO прогресу вивчення карток
   * @throws ResourceNotFoundException якщо користувача або колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public List<LearningProgressDTO> getCardsForReviewByCollection(Long userId, Long collectionId, int limit) {
    log.debug("Getting cards for review for user: {} from collection: {}", userId, collectionId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Перевірка доступу до колекції
    if (!collection.getIsPublic() && !collection.getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    LocalDateTime now = LocalDateTime.now();
    List<LearningProgress> reviewProgress = learningProgressRepository.findCardsForReviewByCollection(user, collectionId, now);

    return reviewProgress.stream()
            .limit(limit)
            .map(this::convertProgressToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати прогрес вивчення для картки
   * @param userId ідентифікатор користувача
   * @param cardId ідентифікатор картки
   * @return DTO прогресу вивчення
   * @throws ResourceNotFoundException якщо користувача, картку або прогрес не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional(readOnly = true)
  public LearningProgressDTO getProgressForCard(Long userId, Long cardId) {
    log.debug("Getting learning progress for user: {} and card: {}", userId, cardId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

    // Перевірка доступу до картки
    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }

    LearningProgress progress = learningProgressRepository.findByUserAndCard(user, card)
            .orElseThrow(() -> new ResourceNotFoundException("Learning progress not found for user: " + userId + " and card: " + cardId));

    return convertProgressToDto(progress);
  }

  /**
   * Отримати прогрес вивчення для користувача
   * @param userId ідентифікатор користувача
   * @param pageable параметри пагінації
   * @return сторінка DTO прогресу вивчення
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public Page<LearningProgressDTO> getUserProgress(Long userId, Pageable pageable) {
    log.debug("Getting learning progress for user: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Page<LearningProgress> progressPage = learningProgressRepository.findByUser(user, pageable);
    List<LearningProgressDTO> progressDTOs = progressPage.getContent().stream()
            .map(this::convertProgressToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(progressDTOs, pageable, progressPage.getTotalElements());
  }

  /**
   * Відповідь на картку (оновлення прогресу вивчення)
   * @param userId ідентифікатор користувача
   * @param cardAnswerRequest дані відповіді
   * @return DTO оновленого прогресу вивчення
   * @throws ResourceNotFoundException якщо користувача або картку не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional
  public LearningProgressDTO answerCard(Long userId, CardDTO.CardAnswerRequest cardAnswerRequest) {
    log.debug("Processing answer for user: {} and card: {}", userId, cardAnswerRequest.getCardId());
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Card card = cardRepository.findById(cardAnswerRequest.getCardId())
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardAnswerRequest.getCardId()));

    // Перевірка доступу до картки
    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }

    // Отримуємо або створюємо прогрес вивчення
    LearningProgress progress = learningProgressRepository.findByUserAndCard(user, card)
            .orElseGet(() -> {
              LearningProgress newProgress = LearningProgress.builder()
                      .user(user)
                      .card(card)
                      .build();
              return learningProgressRepository.save(newProgress);
            });

    // Оновлюємо прогрес на основі відповіді
    progress.setRepetitionCount(progress.getRepetitionCount() + 1);

    if (cardAnswerRequest.getIsCorrect()) {
      progress.setCorrectAnswers(progress.getCorrectAnswers() + 1);
    } else {
      progress.setIncorrectAnswers(progress.getIncorrectAnswers() + 1);
    }

    // Оновлюємо фактор легкості (ease factor) за алгоритмом SuperMemo SM-2
    BigDecimal easeFactor = progress.getEaseFactor();
    if (cardAnswerRequest.getIsCorrect()) {
      // Збільшуємо фактор легкості при правильній відповіді
      BigDecimal newEase = easeFactor.add(BigDecimal.valueOf(0.1));
      progress.setEaseFactor(newEase.min(BigDecimal.valueOf(2.5)));
    } else {
      // Зменшуємо фактор легкості при неправильній відповіді
      BigDecimal newEase = easeFactor.subtract(BigDecimal.valueOf(0.2));
      progress.setEaseFactor(newEase.max(BigDecimal.valueOf(1.3)));
    }

    // Оновлюємо статус вивчення та дату наступного повторення
    updateLearningStatus(progress, cardAnswerRequest.getIsCorrect());

    // Записуємо час останнього повторення
    progress.setLastReviewedAt(LocalDateTime.now());

    LearningProgress updatedProgress = learningProgressRepository.save(progress);

    // Записуємо історію повторення
    recordReviewHistory(user, card, cardAnswerRequest.getIsCorrect(), cardAnswerRequest.getResponseTimeMs());

    log.info("Learning progress updated for user: {} and card: {}", userId, cardAnswerRequest.getCardId());

    return convertProgressToDto(updatedProgress);
  }

  /**
   * Скинути прогрес вивчення для картки
   * @param userId ідентифікатор користувача
   * @param cardId ідентифікатор картки
   * @throws ResourceNotFoundException якщо користувача, картку або прогрес не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional
  public void resetCardProgress(Long userId, Long cardId) {
    log.debug("Resetting progress for user: {} and card: {}", userId, cardId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

    // Перевірка доступу до картки
    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }

    LearningProgress progress = learningProgressRepository.findByUserAndCard(user, card)
            .orElseThrow(() -> new ResourceNotFoundException("Learning progress not found for user: " + userId + " and card: " + cardId));

    // Скидаємо прогрес
    progress.setRepetitionCount(0);
    progress.setCorrectAnswers(0);
    progress.setIncorrectAnswers(0);
    progress.setEaseFactor(BigDecimal.valueOf(2.5));
    progress.setLearningStatus(LearningStatus.NEW);
    progress.setNextReviewDate(LocalDateTime.now());

    learningProgressRepository.save(progress);
    log.info("Progress reset for user: {} and card: {}", userId, cardId);
  }

  /**
   * Скинути прогрес вивчення для всіх карток колекції
   * @param userId ідентифікатор користувача
   * @param collectionId ідентифікатор колекції
   * @throws ResourceNotFoundException якщо користувача або колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional
  public void resetCollectionProgress(Long userId, Long collectionId) {
    log.debug("Resetting progress for user: {} and collection: {}", userId, collectionId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Перевірка доступу до колекції
    if (!collection.getIsPublic() && !collection.getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    List<LearningProgress> progressList = learningProgressRepository.findByUserAndCollectionId(user, collectionId);

    for (LearningProgress progress : progressList) {
      // Скидаємо прогрес
      progress.setRepetitionCount(0);
      progress.setCorrectAnswers(0);
      progress.setIncorrectAnswers(0);
      progress.setEaseFactor(BigDecimal.valueOf(2.5));
      progress.setLearningStatus(LearningStatus.NEW);
      progress.setNextReviewDate(LocalDateTime.now());
    }

    if (!progressList.isEmpty()) {
      learningProgressRepository.saveAll(progressList);
      log.info("Progress reset for user: {} and collection: {}", userId, collectionId);
    }
  }

  /**
   * Отримати статистику прогресу за статусами
   * @param userId ідентифікатор користувача
   * @return список об'єктів DTO статистики
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public List<LearningProgressDTO.StatusStatisticsDTO> getStatusStatistics(Long userId) {
    log.debug("Getting status statistics for user: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    List<Object[]> statistics = learningProgressRepository.getStatusStatisticsByUser(user);

    return statistics.stream()
            .map(row -> {
              LearningStatus status = (LearningStatus) row[0];
              Long count = (Long) row[1];

              return LearningProgressDTO.StatusStatisticsDTO.builder()
                      .status(status.name())
                      .count(count)
                      .build();
            })
            .collect(Collectors.toList());
  }

  /**
   * Отримати статистику прогресу за статусами для колекції
   * @param userId ідентифікатор користувача
   * @param collectionId ідентифікатор колекції
   * @return список об'єктів DTO статистики
   * @throws ResourceNotFoundException якщо користувача або колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public List<LearningProgressDTO.StatusStatisticsDTO> getStatusStatisticsForCollection(Long userId, Long collectionId) {
    log.debug("Getting status statistics for user: {} and collection: {}", userId, collectionId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Перевірка доступу до колекції
    if (!collection.getIsPublic() && !collection.getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    List<Object[]> statistics = learningProgressRepository.getStatusStatisticsByUserAndCollection(user, collectionId);

    return statistics.stream()
            .map(row -> {
              LearningStatus status = (LearningStatus) row[0];
              Long count = (Long) row[1];

              return LearningProgressDTO.StatusStatisticsDTO.builder()
                      .status(status.name())
                      .count(count)
                      .build();
            })
            .collect(Collectors.toList());
  }

  /**
   * Оновити статус вивчення та дату наступного повторення
   * @param progress прогрес вивчення
   * @param isCorrect чи була відповідь правильною
   */
  private void updateLearningStatus(LearningProgress progress, boolean isCorrect) {
    LocalDateTime now = LocalDateTime.now();

    // Визначаємо новий статус вивчення та інтервал до наступного повторення
    switch (progress.getLearningStatus()) {
    case NEW:
      if (isCorrect) {
        progress.setLearningStatus(LearningStatus.LEARNING);
        // Для нової картки перше повторення через 10 хвилин
        progress.setNextReviewDate(now.plusMinutes(10));
      } else {
        // Залишаємо як NEW, але повторюємо через 5 хвилин
        progress.setNextReviewDate(now.plusMinutes(5));
      }
      break;

    case LEARNING:
      if (isCorrect) {
        // Якщо це третя правильна відповідь підряд, переводимо в REVIEW
        if (progress.getCorrectAnswers() >= 3) {
          progress.setLearningStatus(LearningStatus.REVIEW);
          // Перше повторення через 1 день
          progress.setNextReviewDate(now.plusDays(1));
        } else {
          // Інакше повторюємо через 30 хвилин
          progress.setNextReviewDate(now.plusMinutes(30));
        }
      } else {
        // При неправильній відповіді повторюємо через 10 хвилин
        progress.setNextReviewDate(now.plusMinutes(10));
      }
      break;

    case REVIEW:
      // Визначаємо інтервал на основі фактора легкості (ease factor)
      double easeFactor = progress.getEaseFactor().doubleValue();

      if (isCorrect) {
        // Якщо це п'ята правильна відповідь підряд, переводимо в KNOWN
        if (progress.getCorrectAnswers() >= 5) {
          progress.setLearningStatus(LearningStatus.KNOWN);
          // Наступне повторення через 7-14 днів залежно від фактора легкості
          int daysInterval = (int) Math.round(7 * easeFactor);
          progress.setNextReviewDate(now.plusDays(daysInterval));
        } else {
          // Інакше повторюємо через 1-5 днів залежно від фактора легкості
          int daysInterval = (int) Math.round(easeFactor);
          progress.setNextReviewDate(now.plusDays(Math.max(1, daysInterval)));
        }
      } else {
        // При неправильній відповіді повертаємось до LEARNING
        progress.setLearningStatus(LearningStatus.LEARNING);
        progress.setNextReviewDate(now.plusHours(3));
      }
      break;

    case KNOWN:
      if (isCorrect) {
        // Визначаємо інтервал на основі фактора легкості (ease factor)
        double knownEaseFactor = progress.getEaseFactor().doubleValue();
        // Наступне повторення через 14-60 днів залежно від фактора легкості
        int daysInterval = (int) Math.round(14 * knownEaseFactor);
        progress.setNextReviewDate(now.plusDays(Math.min(60, daysInterval)));
      } else {
        // При неправильній відповіді повертаємось до REVIEW
        progress.setLearningStatus(LearningStatus.REVIEW);
        progress.setNextReviewDate(now.plusDays(1));
      }
      break;
    }
  }

  /**
   * Записати історію повторення картки
   * @param user користувач
   * @param card картка
   * @param isCorrect чи була відповідь правильною
   * @param responseTimeMs час відповіді в мілісекундах
   */
  private void recordReviewHistory(User user, Card card, boolean isCorrect, Integer responseTimeMs) {
    try {
      LearningHistory history = LearningHistory.builder()
              .user(user)
              .card(card)
              .actionType(ActionType.REVIEW)
              .isCorrect(isCorrect)
              .responseTimeMs(responseTimeMs)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);

      // Оновлюємо статистику вивчення для користувача
      UserStatistics statistics = userStatisticsRepository.findByUser(user)
              .orElseGet(() -> {
                UserStatistics newStats = UserStatistics.builder().user(user).build();
                return userStatisticsRepository.save(newStats);
              });

      // Оновлюємо час вивчення
      if (responseTimeMs != null) {
        int studyMinutes = (int) Math.ceil(responseTimeMs / 60000.0);
        statistics.setTotalStudyTimeMinutes(statistics.getTotalStudyTimeMinutes() + studyMinutes);
      }

      userStatisticsRepository.save(statistics);
    } catch (Exception e) {
      log.error("Error recording review history", e);
    }
  }

  /**
   * Конвертувати сутність прогресу вивчення в DTO
   * @param progress сутність прогресу вивчення
   * @return DTO прогресу вивчення
   */
  private LearningProgressDTO convertProgressToDto(LearningProgress progress) {
    LearningProgressDTO progressDTO = modelMapper.map(progress, LearningProgressDTO.class);

    // Додаткова інформація про користувача
    progressDTO.setUserId(progress.getUser().getId());

    // Додаткова інформація про картку
    Card card = progress.getCard();
    progressDTO.setCardId(card.getId());
    progressDTO.setFrontText(card.getFrontText());
    progressDTO.setBackText(card.getBackText());
    progressDTO.setCollectionId(card.getCollection().getId());
    progressDTO.setCollectionName(card.getCollection().getName());

    // Додаткова інформація про статус вивчення
    progressDTO.setLearningStatus(progress.getLearningStatus().name());

    // Розрахунок відсотка успішності
    int totalAnswers = progress.getCorrectAnswers() + progress.getIncorrectAnswers();
    if (totalAnswers > 0) {
      double successRate = (double) progress.getCorrectAnswers() / totalAnswers * 100;
      progressDTO.setSuccessRate(BigDecimal.valueOf(successRate)
              .setScale(2, RoundingMode.HALF_UP).doubleValue());
    } else {
      progressDTO.setSuccessRate(0.0);
    }

    return progressDTO;
  }
}
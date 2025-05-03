package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.LearningProgressDTO;
import com.norbert.twaincards.entity.*;
import com.norbert.twaincards.entity.enumeration.ActionType;
import com.norbert.twaincards.entity.enumeration.LearningStatus;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UnauthorizedAccessException;
import com.norbert.twaincards.repository.*;
import com.norbert.twaincards.util.SecurityUtils;
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
  private final SecurityUtils securityUtils;

  @Transactional(readOnly = true)
  public List<CardDTO> getCardsToLearn(Long collectionId, int limit) {
    User user = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    List<Card> cardsInCollection = cardRepository.findByCollection(collection);
    List<LearningProgress> existingProgress = learningProgressRepository.findByUserAndCollectionId(user, collectionId);

    Map<Long, LearningProgress> cardProgressMap = existingProgress.stream()
            .collect(Collectors.toMap(progress -> progress.getCard().getId(), progress -> progress));

    LocalDateTime now = LocalDateTime.now();

    return cardsInCollection.stream()
            .filter(card -> {
              LearningProgress progress = cardProgressMap.get(card.getId());
              return progress == null ||
                      (progress.getNextReviewDate() != null && !progress.getNextReviewDate().isAfter(now));
            })
            .limit(limit)
            .map(card -> {
              CardDTO cardDTO = modelMapper.map(card, CardDTO.class);
              cardDTO.setCollectionId(card.getCollection().getId());

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

  @Transactional(readOnly = true)
  public List<LearningProgressDTO> getCardsForReview(int limit) {
    User user = securityUtils.getCurrentUser();
    LocalDateTime now = LocalDateTime.now();
    List<LearningProgress> reviewProgress = learningProgressRepository.findCardsForReview(user, now);

    return reviewProgress.stream()
            .limit(limit)
            .map(this::convertProgressToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<LearningProgressDTO> getCardsForReviewByCollection(Long collectionId, int limit) {
    User user = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    LocalDateTime now = LocalDateTime.now();
    List<LearningProgress> reviewProgress = learningProgressRepository.findCardsForReviewByCollection(user, collectionId, now);

    return reviewProgress.stream()
            .limit(limit)
            .map(this::convertProgressToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public LearningProgressDTO getProgressForCard(Long cardId) {
    User user = securityUtils.getCurrentUser();
    Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }

    LearningProgress progress = learningProgressRepository.findByUserAndCard(user, card)
            .orElseThrow(() -> new ResourceNotFoundException("Learning progress not found for user: " + user.getId() + " and card: " + cardId));

    return convertProgressToDto(progress);
  }

  @Transactional(readOnly = true)
  public Page<LearningProgressDTO> getUserProgress(Pageable pageable) {
    User user = securityUtils.getCurrentUser();
    Page<LearningProgress> progressPage = learningProgressRepository.findByUser(user, pageable);
    List<LearningProgressDTO> progressDTOs = progressPage.getContent().stream()
            .map(this::convertProgressToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(progressDTOs, pageable, progressPage.getTotalElements());
  }

  @Transactional
  public LearningProgressDTO answerCard(CardDTO.CardAnswerRequest cardAnswerRequest) {
    User user = securityUtils.getCurrentUser();
    Card card = cardRepository.findById(cardAnswerRequest.getCardId())
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardAnswerRequest.getCardId()));

    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }

    LearningProgress progress = learningProgressRepository.findByUserAndCard(user, card)
            .orElseGet(() -> {
              LearningProgress newProgress = LearningProgress.builder()
                      .user(user)
                      .card(card)
                      .build();
              return learningProgressRepository.save(newProgress);
            });

    progress.setRepetitionCount(progress.getRepetitionCount() + 1);

    if (cardAnswerRequest.getIsCorrect()) {
      progress.setCorrectAnswers(progress.getCorrectAnswers() + 1);
    } else {
      progress.setIncorrectAnswers(progress.getIncorrectAnswers() + 1);
    }

    BigDecimal easeFactor = progress.getEaseFactor();
    if (cardAnswerRequest.getIsCorrect()) {
      BigDecimal newEase = easeFactor.add(BigDecimal.valueOf(0.1));
      progress.setEaseFactor(newEase.min(BigDecimal.valueOf(2.5)));
    } else {
      BigDecimal newEase = easeFactor.subtract(BigDecimal.valueOf(0.2));
      progress.setEaseFactor(newEase.max(BigDecimal.valueOf(1.3)));
    }

    updateLearningStatus(progress, cardAnswerRequest.getIsCorrect());
    progress.setLastReviewedAt(LocalDateTime.now());

    LearningProgress updatedProgress = learningProgressRepository.save(progress);
    recordReviewHistory(user, card, cardAnswerRequest.getIsCorrect(), cardAnswerRequest.getResponseTimeMs());

    log.info("Learning progress updated for user: {} and card: {}", user.getId(), cardAnswerRequest.getCardId());

    return convertProgressToDto(updatedProgress);
  }

  @Transactional
  public void resetCardProgress(Long cardId) {
    User user = securityUtils.getCurrentUser();
    Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }

    LearningProgress progress = learningProgressRepository.findByUserAndCard(user, card)
            .orElseThrow(() -> new ResourceNotFoundException("Learning progress not found for user: " + user.getId() + " and card: " + cardId));

    progress.setRepetitionCount(0);
    progress.setCorrectAnswers(0);
    progress.setIncorrectAnswers(0);
    progress.setEaseFactor(BigDecimal.valueOf(2.5));
    progress.setLearningStatus(LearningStatus.NEW);
    progress.setNextReviewDate(LocalDateTime.now());

    learningProgressRepository.save(progress);
    log.info("Progress reset for user: {} and card: {}", user.getId(), cardId);
  }

  @Transactional
  public void resetCollectionProgress(Long collectionId) {
    User user = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(user.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    List<LearningProgress> progressList = learningProgressRepository.findByUserAndCollectionId(user, collectionId);

    for (LearningProgress progress : progressList) {
      progress.setRepetitionCount(0);
      progress.setCorrectAnswers(0);
      progress.setIncorrectAnswers(0);
      progress.setEaseFactor(BigDecimal.valueOf(2.5));
      progress.setLearningStatus(LearningStatus.NEW);
      progress.setNextReviewDate(LocalDateTime.now());
    }

    if (!progressList.isEmpty()) {
      learningProgressRepository.saveAll(progressList);
      log.info("Progress reset for user: {} and collection: {}", user.getId(), collectionId);
    }
  }

  @Transactional(readOnly = true)
  public List<LearningProgressDTO.StatusStatisticsDTO> getStatusStatistics() {
    User user = securityUtils.getCurrentUser();
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

  @Transactional(readOnly = true)
  public List<LearningProgressDTO.StatusStatisticsDTO> getStatusStatisticsForCollection(Long collectionId) {
    User user = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(user.getId())) {
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

  private void updateLearningStatus(LearningProgress progress, boolean isCorrect) {
    LocalDateTime now = LocalDateTime.now();

    switch (progress.getLearningStatus()) {
    case NEW:
      if (isCorrect) {
        progress.setLearningStatus(LearningStatus.LEARNING);
        progress.setNextReviewDate(now.plusMinutes(10));
      } else {
        progress.setNextReviewDate(now.plusMinutes(5));
      }
      break;

    case LEARNING:
      if (isCorrect) {
        if (progress.getCorrectAnswers() >= 3) {
          progress.setLearningStatus(LearningStatus.REVIEW);
          progress.setNextReviewDate(now.plusDays(1));
        } else {
          progress.setNextReviewDate(now.plusMinutes(30));
        }
      } else {
        progress.setNextReviewDate(now.plusMinutes(10));
      }
      break;

    case REVIEW:
      double easeFactor = progress.getEaseFactor().doubleValue();

      if (isCorrect) {
        if (progress.getCorrectAnswers() >= 5) {
          progress.setLearningStatus(LearningStatus.KNOWN);
          int daysInterval = (int) Math.round(7 * easeFactor);
          progress.setNextReviewDate(now.plusDays(daysInterval));
        } else {
          int daysInterval = (int) Math.round(easeFactor);
          progress.setNextReviewDate(now.plusDays(Math.max(1, daysInterval)));
        }
      } else {
        progress.setLearningStatus(LearningStatus.LEARNING);
        progress.setNextReviewDate(now.plusHours(3));
      }
      break;

    case KNOWN:
      if (isCorrect) {
        double knownEaseFactor = progress.getEaseFactor().doubleValue();
        int daysInterval = (int) Math.round(14 * knownEaseFactor);
        progress.setNextReviewDate(now.plusDays(Math.min(60, daysInterval)));
      } else {
        progress.setLearningStatus(LearningStatus.REVIEW);
        progress.setNextReviewDate(now.plusDays(1));
      }
      break;
    }
  }

  private void recordReviewHistory(User user, Card card, boolean isCorrect, Integer responseTimeMs) {
    try {
      LearningHistory history = LearningHistory.builder()
              .user(user)
              .card(card)
              .actionType(ActionType.REVIEW)
              .isCorrect(isCorrect)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);

      UserStatistics statistics = userStatisticsRepository.findByUser(user)
              .orElseGet(() -> {
                UserStatistics newStats = UserStatistics.builder().user(user).build();
                return userStatisticsRepository.save(newStats);
              });

      userStatisticsRepository.save(statistics);
    } catch (Exception e) {
      log.error("Error recording review history", e);
    }
  }

  private LearningProgressDTO convertProgressToDto(LearningProgress progress) {
    LearningProgressDTO progressDTO = modelMapper.map(progress, LearningProgressDTO.class);

    progressDTO.setUserId(progress.getUser().getId());

    Card card = progress.getCard();
    progressDTO.setCardId(card.getId());
    progressDTO.setFrontText(card.getFrontText());
    progressDTO.setBackText(card.getBackText());
    progressDTO.setCollectionId(card.getCollection().getId());
    progressDTO.setCollectionName(card.getCollection().getName());

    progressDTO.setLearningStatus(progress.getLearningStatus().name());

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
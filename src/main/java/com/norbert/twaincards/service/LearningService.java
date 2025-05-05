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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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
import java.time.LocalDate;
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
  private final StudySessionRepository studySessionRepository;

  @PersistenceContext
  private EntityManager entityManager;

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
  public List<LearningProgressDTO> getCardsForReview() {
    User user = securityUtils.getCurrentUser();
    LocalDateTime now = LocalDateTime.now();
    List<LearningProgress> reviewProgress = learningProgressRepository.findCardsForReview(user, now);

    return reviewProgress.stream()
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

    updateLearningStatus(progress, cardAnswerRequest.getIsCorrect());
    progress.setLastReviewedAt(LocalDateTime.now());

    LearningProgress updatedProgress = learningProgressRepository.save(progress);
    
    // Get the study session if a session ID was provided
    StudySession studySession = null;
    if (cardAnswerRequest.getSessionId() != null) {
      studySession = studySessionRepository.findById(cardAnswerRequest.getSessionId())
              .orElse(null);
      
      if (studySession == null) {
        log.warn("Study session with ID {} not found", cardAnswerRequest.getSessionId());
      }
    }
    
    recordReviewHistory(card, cardAnswerRequest.getIsCorrect(), user, studySession);

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
    int repetitionCount = progress.getRepetitionCount();
    
    // For incorrect answers, we reset or go back one step
    if (!isCorrect) {
      switch (progress.getLearningStatus()) {
        case NEW:
          // Stay in NEW status, but review sooner
          progress.setNextReviewDate(now.plusMinutes(5));
          break;
          
        case LEARNING:
          // Stay in LEARNING, but review sooner
          progress.setNextReviewDate(now.plusMinutes(10));
          break;
          
        case REVIEW:
          // Drop back to LEARNING
          progress.setLearningStatus(LearningStatus.LEARNING);
          progress.setNextReviewDate(now.plusHours(1));
          break;
          
        case KNOWN:
          // Drop back to REVIEW
          progress.setLearningStatus(LearningStatus.REVIEW);
          progress.setNextReviewDate(now.plusDays(1));
          break;
      }
      return;
    }
    
    // For correct answers, we advance based on repetition count
    switch (progress.getLearningStatus()) {
      case NEW:
        progress.setLearningStatus(LearningStatus.LEARNING);
        progress.setNextReviewDate(now.plusHours(1)); // First time correct: repeat in 1 hour
        break;
        
      case LEARNING:
        if (repetitionCount >= 2) {
          progress.setLearningStatus(LearningStatus.REVIEW);
          progress.setNextReviewDate(now.plusHours(6)); // Second time correct: repeat in 6 hours
        } else {
          progress.setNextReviewDate(now.plusHours(1)); // Still learning
        }
        break;
        
      case REVIEW:
        if (repetitionCount >= 3) {
          progress.setLearningStatus(LearningStatus.KNOWN);
          progress.setNextReviewDate(now.plusDays(1)); // Third time correct: repeat in 1 day
        } else {
          progress.setNextReviewDate(now.plusHours(6)); // Still in review
        }
        break;
        
      case KNOWN:
        progress.setNextReviewDate(now.plusDays(7)); // Fourth time and beyond: repeat in 7 days
        break;
    }
  }

  @Transactional
  public void recordReviewHistory(Card card, boolean isCorrect, User user, StudySession studySession) {
    try {
      // Create learning history entry
      LearningHistory history = LearningHistory.builder()
              .user(user)
              .card(card)
              .actionType(ActionType.REVIEW)
              .isCorrect(isCorrect)
              .studySession(studySession)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);

      // Update user statistics
      UserStatistics statistics = userStatisticsRepository.findByUser(user)
              .orElse(UserStatistics.builder().user(user).build());

      // Update learning streak
      LocalDate today = LocalDate.now();
      if (statistics.getLastStudyDate() == null || !statistics.getLastStudyDate().equals(today)) {
        if (statistics.getLastStudyDate() != null && 
            statistics.getLastStudyDate().plusDays(1).equals(today)) {
          // Increment streak only if last study was yesterday
          statistics.setLearningStreakDays(statistics.getLearningStreakDays() + 1);
        } else if (statistics.getLastStudyDate() == null || 
                  !statistics.getLastStudyDate().equals(today)) {
          // Reset streak if more than 1 day gap
          statistics.setLearningStreakDays(1);
        }
        statistics.setLastStudyDate(today);
      }

      // Get the specific collection for this card
      Collection collection = card.getCollection();
      Long collectionId = collection.getId();
      
      // Get total cards count for this collection, including public collections and user's own collections
      Long totalCards = cardRepository.countByCollectionAndUserAccess(collection, user);
      
      // Count learned, learning, and review cards for this specific collection
      Long learnedCards = learningProgressRepository.countKnownCardsByUserAndCollection(user, collectionId);
      Long learningCards = learningProgressRepository.countLearningCardsByUserAndCollection(user, collectionId);
      Long reviewCards = learningProgressRepository.countReviewCardsByUserAndCollection(user, collectionId);
      
      // Total in-progress cards for this collection
      Long inProgressCards = learningCards + reviewCards;
      
      // Update statistics for this collection
      statistics.setTotalCards(totalCards.intValue());
      statistics.setLearnedCards(learnedCards.intValue());
      statistics.setCardsInProgress(inProgressCards.intValue());
      
      // Calculate cards to learn (total - learned - in progress) for this collection
      int toLearnCards = totalCards.intValue() - learnedCards.intValue() - inProgressCards.intValue();
      statistics.setCardsToLearn(Math.max(0, toLearnCards));
      
      userStatisticsRepository.save(statistics);
    } catch (Exception e) {
      log.error("Error recording review history", e);
      // Continue execution despite the error
    }
  }

  private LearningProgressDTO convertProgressToDto(LearningProgress progress) {
    LearningProgressDTO progressDTO = modelMapper.map(progress, LearningProgressDTO.class);

    progressDTO.setUserId(progress.getUser().getId());

    try {
      Card card = progress.getCard();
      progressDTO.setCardId(card.getId());
      progressDTO.setFrontText(card.getFrontText());
      progressDTO.setBackText(card.getBackText());
      progressDTO.setCollectionId(card.getCollection().getId());
      progressDTO.setCollectionName(card.getCollection().getName());
    } catch (EntityNotFoundException ex) {
      // This means the user doesn't have access to this card due to role-based security
      log.warn("User does not have access to card: {}, setting minimal info", progress.getCard().getId());
      // Set just the ID which we know, but mark text fields as "Access restricted"
      progressDTO.setCardId(progress.getCard().getId());
      progressDTO.setFrontText("[Access restricted]");
      progressDTO.setBackText("[Access restricted]");
      // Leave collection fields null
    }

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
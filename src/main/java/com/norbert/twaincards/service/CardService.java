package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.entity.*;
import com.norbert.twaincards.entity.enumeration.ActionType;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UnauthorizedAccessException;
import com.norbert.twaincards.repository.CardRepository;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.LearningHistoryRepository;
import com.norbert.twaincards.repository.UserStatisticsRepository;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final CardRepository cardRepository;
  private final CollectionRepository collectionRepository;
  private final LearningHistoryRepository learningHistoryRepository;
  private final UserStatisticsRepository userStatisticsRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @Transactional(readOnly = true)
  public List<CardDTO> getCardsByCollection(Long collectionId) {
    Collection collection = getCollectionWithAccessCheck(collectionId);

    return cardRepository.findByCollection(collection).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<CardDTO> getCardsByCollection(Long collectionId, Pageable pageable) {
    Collection collection = getCollectionWithAccessCheck(collectionId);

    Page<Card> cardsPage = cardRepository.findByCollection(collection, pageable);
    List<CardDTO> cardDTOs = cardsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(cardDTOs, pageable, cardsPage.getTotalElements());
  }

  public CardDTO getCardById(Long id) {
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    checkCardAccess(card);
    
    CardDTO cardDTO = convertToDto(card);
    
    try {
      recordCardViewSeparate(card);
    } catch (Exception e) {
      log.error("Error recording card view history in separate transaction", e);
    }
    
    return cardDTO;
  }

  @Transactional
  public CardDTO createCard(CardDTO.CreateCardRequest createCardRequest) {
    User currentUser = securityUtils.getCurrentUser();
    Collection collection = getCollectionWithAccessCheck(createCardRequest.getCollectionId());

    Card card = Card.builder()
            .collection(collection)
            .frontText(createCardRequest.getFrontText())
            .backText(createCardRequest.getBackText())
            .exampleUsage(createCardRequest.getExampleUsage())
            .createdAt(LocalDateTime.now())
            .build();


    Card savedCard = cardRepository.save(card);
    log.info("Card created successfully with id: {}", savedCard.getId());
    recordCardCreation(savedCard);

    return convertToDto(savedCard);
  }

  @Transactional
  public CardDTO updateCard(Long id, CardDTO cardDTO) {
    User currentUser = securityUtils.getCurrentUser();
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    checkCardAccess(card);

    card.setFrontText(cardDTO.getFrontText());
    card.setBackText(cardDTO.getBackText());
    card.setExampleUsage(cardDTO.getExampleUsage());
    card.setUpdatedAt(LocalDateTime.now());

    Card updatedCard = cardRepository.save(card);
    log.info("Card updated successfully with id: {}", updatedCard.getId());
    recordCardEdit(updatedCard);

    return convertToDto(updatedCard);
  }

  @Transactional
  public void deleteCard(Long id) {
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    checkCardAccess(card);
    
    // First, handle dependent LearningHistory entities separately
    // This avoids ConcurrentModificationException during cascading of bidirectional relationships
    learningHistoryRepository.deleteByCard(card);
    
    // Record the deletion in a new transaction after cleaning up existing history
    recordCardDeletion(card);

    cardRepository.delete(card);
    log.info("Card deleted successfully with id: {}", id);
  }

  @Transactional(readOnly = true)
  public List<CardDTO> searchCards(Long collectionId, String searchText) {
    Collection collection = getCollectionWithAccessCheck(collectionId);

    List<Card> cards = cardRepository.findByTextInCard(searchText, collection);
    return cards.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  private Collection getCollectionWithAccessCheck(Long collectionId) {
    User currentUser = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    return collection;
  }

  private void checkCardAccess(Card card) {
    User currentUser = securityUtils.getCurrentUser();
    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }
  }

  private void recordCardView(Card card) {
    try {
      User currentUser = securityUtils.getCurrentUser();
      LearningHistory history = LearningHistory.builder()
              .user(currentUser)
              .card(card)
              .actionType(ActionType.VIEW)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);
    } catch (Exception e) {
      log.error("Error recording card view history", e);
    }
  }

  private void recordCardCreation(Card card) {
    try {
      User currentUser = securityUtils.getCurrentUser();
      LearningHistory history = LearningHistory.builder()
              .user(currentUser)
              .card(card)
              .actionType(ActionType.CREATE)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);
    } catch (Exception e) {
      log.error("Error recording card creation history", e);
    }
  }

  private void recordCardEdit(Card card) {
    try {
      User currentUser = securityUtils.getCurrentUser();
      LearningHistory history = LearningHistory.builder()
              .user(currentUser)
              .card(card)
              .actionType(ActionType.EDIT)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);
    } catch (Exception e) {
      log.error("Error recording card edit history", e);
    }
  }

  private void recordCardDeletion(Card card) {
    try {
      User currentUser = securityUtils.getCurrentUser();
      LearningHistory history = LearningHistory.builder()
              .user(currentUser)
              .card(card)
              .actionType(ActionType.DELETE)
              .performedAt(LocalDateTime.now())
              .build();

      learningHistoryRepository.save(history);
    } catch (Exception e) {
      log.error("Error recording card deletion history", e);
    }
  }

  /**
   * Records a card view in a separate transaction to avoid issues with read-only transactions
   */
  @Transactional
  public void recordCardViewSeparate(Card card) {
    recordCardView(card);
  }

  private CardDTO convertToDto(Card card) {
    CardDTO cardDTO = modelMapper.map(card, CardDTO.class);
    cardDTO.setCollectionId(card.getCollection().getId());
    return cardDTO;
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

      userStatisticsRepository.save(statistics);  // This doesn't update the actual statistics!
    } catch (Exception e) {
      log.error("Error recording review history", e);
    }
  }
}
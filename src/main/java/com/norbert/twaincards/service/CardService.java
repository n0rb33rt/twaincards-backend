package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.TagDTO;
import com.norbert.twaincards.entity.*;
import com.norbert.twaincards.entity.enumeration.ActionType;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UnauthorizedAccessException;
import com.norbert.twaincards.repository.CardRepository;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.LearningHistoryRepository;
import com.norbert.twaincards.repository.TagRepository;
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
  private final TagRepository tagRepository;
  private final LearningHistoryRepository learningHistoryRepository;
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

  @Transactional(readOnly = true)
  public CardDTO getCardById(Long id) {
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    checkCardAccess(card);
    recordCardView(card);

    return convertToDto(card);
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
            .build();

    if (createCardRequest.getTagNames() != null && !createCardRequest.getTagNames().isEmpty()) {
      for (String tagName : createCardRequest.getTagNames()) {
        // Find tag by name and user or create a new one
        Tag tag = tagRepository.findByNameAndUser(tagName, currentUser)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .name(tagName)
                        .user(currentUser)
                        .build()));
        card.addTag(tag);
      }
    }

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

    if (cardDTO.getTags() != null) {
      Set<Tag> currentTags = new HashSet<>(card.getTags());
      for (Tag tag : currentTags) {
        card.removeTag(tag);
      }

      for (TagDTO tagDTO : cardDTO.getTags()) {
        // Find tag by name and user or create a new one
        Tag tag = tagRepository.findByNameAndUser(tagDTO.getName(), currentUser)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .name(tagDTO.getName())
                        .user(currentUser)
                        .build()));
        card.addTag(tag);
      }
    }

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

  @Transactional(readOnly = true)
  public List<CardDTO> getCardsByTag(Long collectionId, String tagName) {
    User currentUser = securityUtils.getCurrentUser();
    Collection collection = getCollectionWithAccessCheck(collectionId);

    // Find tag by name and user
    Tag tag = tagRepository.findByNameAndUser(tagName, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + tagName));

    List<Card> cards = cardRepository.findByTagAndCollection(tag, collection);
    return cards.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional
  public CardDTO addTagsToCard(Long id, List<String> tagNames) {
    User currentUser = securityUtils.getCurrentUser();
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    checkCardAccess(card);

    for (String tagName : tagNames) {
      // Find tag by name and user or create a new one
      Tag tag = tagRepository.findByNameAndUser(tagName, currentUser)
              .orElseGet(() -> tagRepository.save(Tag.builder()
                      .name(tagName)
                      .user(currentUser)
                      .build()));
      card.addTag(tag);
    }

    card.setUpdatedAt(LocalDateTime.now());
    Card updatedCard = cardRepository.save(card);
    log.info("Tags added successfully to card with id: {}", updatedCard.getId());

    return convertToDto(updatedCard);
  }

  @Transactional
  public CardDTO removeTagFromCard(Long id, String tagName) {
    User currentUser = securityUtils.getCurrentUser();
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    checkCardAccess(card);

    // Find tag by name and user
    Tag tag = tagRepository.findByNameAndUser(tagName, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + tagName));

    card.removeTag(tag);
    card.setUpdatedAt(LocalDateTime.now());
    Card updatedCard = cardRepository.save(card);
    log.info("Tag removed successfully from card with id: {}", updatedCard.getId());

    return convertToDto(updatedCard);
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

  private CardDTO convertToDto(Card card) {
    CardDTO cardDTO = modelMapper.map(card, CardDTO.class);
    cardDTO.setCollectionId(card.getCollection().getId());

    Set<TagDTO> tagDTOs = card.getTags().stream()
            .map(tag -> TagDTO.builder()
                    .id(tag.getId())
                    .name(tag.getName())
                    .build())
            .collect(Collectors.toSet());

    cardDTO.setTags(tagDTOs);
    return cardDTO;
  }
}
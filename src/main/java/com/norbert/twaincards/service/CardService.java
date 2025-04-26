package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.TagDTO;
import com.norbert.twaincards.entity.*;
import com.norbert.twaincards.entity.LearningHistory.ActionType;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з картками
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final CardRepository cardRepository;
  private final CollectionRepository collectionRepository;
  private final UserRepository userRepository;
  private final TagRepository tagRepository;
  private final LearningHistoryRepository learningHistoryRepository;
  private final ModelMapper modelMapper;

  /**
   * Отримати картки колекції
   * @param collectionId ідентифікатор колекції
   * @param userId ідентифікатор користувача для перевірки доступу
   * @return список DTO карток
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public List<CardDTO> getCardsByCollection(Long collectionId, Long userId) {
    log.debug("Getting cards for collection with id: {}", collectionId);
    Collection collection = getCollectionWithAccessCheck(collectionId, userId);

    return cardRepository.findByCollection(collection).stream()
            .map(card -> convertToDto(card, userId))
            .collect(Collectors.toList());
  }

  /**
   * Отримати сторінку карток колекції
   * @param collectionId ідентифікатор колекції
   * @param userId ідентифікатор користувача для перевірки доступу
   * @param pageable параметри пагінації
   * @return сторінка DTO карток
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public Page<CardDTO> getCardsByCollection(Long collectionId, Long userId, Pageable pageable) {
    log.debug("Getting cards page for collection with id: {}", collectionId);
    Collection collection = getCollectionWithAccessCheck(collectionId, userId);

    Page<Card> cardsPage = cardRepository.findByCollection(collection, pageable);
    List<CardDTO> cardDTOs = cardsPage.getContent().stream()
            .map(card -> convertToDto(card, userId))
            .collect(Collectors.toList());

    return new PageImpl<>(cardDTOs, pageable, cardsPage.getTotalElements());
  }

  /**
   * Отримати картку за ідентифікатором
   * @param id ідентифікатор картки
   * @param userId ідентифікатор користувача для перевірки доступу
   * @return DTO картки
   * @throws ResourceNotFoundException якщо картку не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional(readOnly = true)
  public CardDTO getCardById(Long id, Long userId) {
    log.debug("Getting card by id: {}", id);
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    // Перевірка доступу до картки
    checkCardAccess(card, userId);

    // Записуємо історію перегляду картки
    recordCardView(card, userId);

    return convertToDto(card, userId);
  }

  /**
   * Створити нову картку
   * @param userId ідентифікатор користувача
   * @param createCardRequest дані для створення картки
   * @return DTO створеної картки
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional
  public CardDTO createCard(Long userId, CardDTO.CreateCardRequest createCardRequest) {
    log.debug("Creating new card for collection with id: {}", createCardRequest.getCollectionId());

    // Отримання колекції з перевіркою доступу
    Collection collection = getCollectionWithAccessCheck(createCardRequest.getCollectionId(), userId);

    // Створення нової картки
    Card card = Card.builder()
            .collection(collection)
            .frontText(createCardRequest.getFrontText())
            .backText(createCardRequest.getBackText())
            .phoneticText(createCardRequest.getPhoneticText())
            .exampleUsage(createCardRequest.getExampleUsage())
            .build();

    // Додавання тегів
    if (createCardRequest.getTagNames() != null && !createCardRequest.getTagNames().isEmpty()) {
      for (String tagName : createCardRequest.getTagNames()) {
        Tag tag = tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
        card.addTag(tag);
      }
    }

    Card savedCard = cardRepository.save(card);
    log.info("Card created successfully with id: {}", savedCard.getId());

    // Записуємо історію створення картки
    recordCardCreation(savedCard, userId);

    return convertToDto(savedCard, userId);
  }

  /**
   * Оновити картку
   * @param id ідентифікатор картки
   * @param userId ідентифікатор користувача
   * @param cardDTO нові дані картки
   * @return DTO оновленої картки
   * @throws ResourceNotFoundException якщо картку не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional
  public CardDTO updateCard(Long id, Long userId, CardDTO cardDTO) {
    log.debug("Updating card with id: {}", id);
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    // Перевірка доступу до картки
    checkCardAccess(card, userId);

    // Оновлення даних картки
    card.setFrontText(cardDTO.getFrontText());
    card.setBackText(cardDTO.getBackText());
    card.setPhoneticText(cardDTO.getPhoneticText());
    card.setExampleUsage(cardDTO.getExampleUsage());
    card.setImageUrl(cardDTO.getImageUrl());
    card.setAudioUrl(cardDTO.getAudioUrl());
    card.setUpdatedAt(LocalDateTime.now());

    // Оновлення тегів, якщо вказані
    if (cardDTO.getTags() != null) {
      // Видалення всіх поточних тегів
      Set<Tag> currentTags = new HashSet<>(card.getTags());
      for (Tag tag : currentTags) {
        card.removeTag(tag);
      }

      // Додавання нових тегів
      for (TagDTO tagDTO : cardDTO.getTags()) {
        Tag tag = tagRepository.findByName(tagDTO.getName())
                .orElseGet(() -> tagRepository.save(Tag.builder().name(tagDTO.getName()).build()));
        card.addTag(tag);
      }
    }

    Card updatedCard = cardRepository.save(card);
    log.info("Card updated successfully with id: {}", updatedCard.getId());

    // Записуємо історію редагування картки
    recordCardEdit(updatedCard, userId);

    return convertToDto(updatedCard, userId);
  }

  /**
   * Видалити картку
   * @param id ідентифікатор картки
   * @param userId ідентифікатор користувача
   * @throws ResourceNotFoundException якщо картку не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional
  public void deleteCard(Long id, Long userId) {
    log.debug("Deleting card with id: {}", id);
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    // Перевірка доступу до картки
    checkCardAccess(card, userId);

    // Записуємо історію видалення картки
    recordCardDeletion(card, userId);

    cardRepository.delete(card);
    log.info("Card deleted successfully with id: {}", id);
  }

  /**
   * Пошук карток за текстом
   * @param collectionId ідентифікатор колекції
   * @param userId ідентифікатор користувача
   * @param searchText текст для пошуку
   * @return список DTO знайдених карток
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public List<CardDTO> searchCards(Long collectionId, Long userId, String searchText) {
    log.debug("Searching cards in collection with id: {} and text: {}", collectionId, searchText);
    Collection collection = getCollectionWithAccessCheck(collectionId, userId);

    List<Card> cards = cardRepository.findByTextInCard(searchText, collection);
    return cards.stream()
            .map(card -> convertToDto(card, userId))
            .collect(Collectors.toList());
  }

  /**
   * Отримати картки за тегом
   * @param collectionId ідентифікатор колекції
   * @param userId ідентифікатор користувача
   * @param tagName назва тегу
   * @return список DTO карток
   * @throws ResourceNotFoundException якщо колекцію або тег не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  @Transactional(readOnly = true)
  public List<CardDTO> getCardsByTag(Long collectionId, Long userId, String tagName) {
    log.debug("Getting cards by tag: {} in collection with id: {}", tagName, collectionId);
    Collection collection = getCollectionWithAccessCheck(collectionId, userId);

    Tag tag = tagRepository.findByName(tagName)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + tagName));

    List<Card> cards = cardRepository.findByTagAndCollection(tag, collection);
    return cards.stream()
            .map(card -> convertToDto(card, userId))
            .collect(Collectors.toList());
  }

  /**
   * Додати теги до картки
   * @param id ідентифікатор картки
   * @param userId ідентифікатор користувача
   * @param tagNames список назв тегів
   * @return DTO оновленої картки
   * @throws ResourceNotFoundException якщо картку не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional
  public CardDTO addTagsToCard(Long id, Long userId, List<String> tagNames) {
    log.debug("Adding tags to card with id: {}", id);
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    // Перевірка доступу до картки
    checkCardAccess(card, userId);

    // Додавання тегів
    for (String tagName : tagNames) {
      Tag tag = tagRepository.findByName(tagName)
              .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
      card.addTag(tag);
    }

    card.setUpdatedAt(LocalDateTime.now());
    Card updatedCard = cardRepository.save(card);
    log.info("Tags added successfully to card with id: {}", updatedCard.getId());

    return convertToDto(updatedCard, userId);
  }

  /**
   * Видалити тег з картки
   * @param id ідентифікатор картки
   * @param userId ідентифікатор користувача
   * @param tagName назва тегу
   * @return DTO оновленої картки
   * @throws ResourceNotFoundException якщо картку або тег не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  @Transactional
  public CardDTO removeTagFromCard(Long id, Long userId, String tagName) {
    log.debug("Removing tag: {} from card with id: {}", tagName, id);
    Card card = cardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));

    // Перевірка доступу до картки
    checkCardAccess(card, userId);

    Tag tag = tagRepository.findByName(tagName)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + tagName));

    card.removeTag(tag);
    card.setUpdatedAt(LocalDateTime.now());
    Card updatedCard = cardRepository.save(card);
    log.info("Tag removed successfully from card with id: {}", updatedCard.getId());

    return convertToDto(updatedCard, userId);
  }

  /**
   * Отримати колекцію з перевіркою доступу
   * @param collectionId ідентифікатор колекції
   * @param userId ідентифікатор користувача
   * @return сутність колекції
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   */
  private Collection getCollectionWithAccessCheck(Long collectionId, Long userId) {
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Перевірка доступу: колекція повинна належати користувачу або бути публічною
    if (!collection.getIsPublic() && !collection.getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    return collection;
  }

  /**
   * Перевірка доступу до картки
   * @param card сутність картки
   * @param userId ідентифікатор користувача
   * @throws UnauthorizedAccessException якщо користувач не має доступу до картки
   */
  private void checkCardAccess(Card card, Long userId) {
    // Перевірка доступу: картка повинна бути в колекції, яка належить користувачу або є публічною
    if (!card.getCollection().getIsPublic() && !card.getCollection().getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this card");
    }
  }

  /**
   * Записати історію перегляду картки
   * @param card сутність картки
   * @param userId ідентифікатор користувача
   */
  private void recordCardView(Card card, Long userId) {
    try {
      User user = userRepository.findById(userId).orElse(null);
      if (user != null) {
        LearningHistory history = LearningHistory.builder()
                .user(user)
                .card(card)
                .actionType(ActionType.VIEW)
                .performedAt(LocalDateTime.now())
                .build();

        learningHistoryRepository.save(history);
      }
    } catch (Exception e) {
      log.error("Error recording card view history", e);
    }
  }

  /**
   * Записати історію створення картки
   * @param card сутність картки
   * @param userId ідентифікатор користувача
   */
  private void recordCardCreation(Card card, Long userId) {
    try {
      User user = userRepository.findById(userId).orElse(null);
      if (user != null) {
        LearningHistory history = LearningHistory.builder()
                .user(user)
                .card(card)
                .actionType(ActionType.CREATE)
                .performedAt(LocalDateTime.now())
                .build();

        learningHistoryRepository.save(history);
      }
    } catch (Exception e) {
      log.error("Error recording card creation history", e);
    }
  }

  /**
   * Записати історію редагування картки
   * @param card сутність картки
   * @param userId ідентифікатор користувача
   */
  private void recordCardEdit(Card card, Long userId) {
    try {
      User user = userRepository.findById(userId).orElse(null);
      if (user != null) {
        LearningHistory history = LearningHistory.builder()
                .user(user)
                .card(card)
                .actionType(ActionType.EDIT)
                .performedAt(LocalDateTime.now())
                .build();

        learningHistoryRepository.save(history);
      }
    } catch (Exception e) {
      log.error("Error recording card edit history", e);
    }
  }

  /**
   * Записати історію видалення картки
   * @param card сутність картки
   * @param userId ідентифікатор користувача
   */
  private void recordCardDeletion(Card card, Long userId) {
    try {
      User user = userRepository.findById(userId).orElse(null);
      if (user != null) {
        LearningHistory history = LearningHistory.builder()
                .user(user)
                .card(card)
                .actionType(ActionType.DELETE)
                .performedAt(LocalDateTime.now())
                .build();

        learningHistoryRepository.save(history);
      }
    } catch (Exception e) {
      log.error("Error recording card deletion history", e);
    }
  }

  /**
   * Конвертувати сутність картки в DTO
   * @param card сутність картки
   * @param userId ідентифікатор користувача для отримання прогресу вивчення
   * @return DTO картки
   */
  private CardDTO convertToDto(Card card, Long userId) {
    CardDTO cardDTO = modelMapper.map(card, CardDTO.class);

    // Додаємо інформацію про колекцію
    cardDTO.setCollectionId(card.getCollection().getId());

    // Додаємо інформацію про теги
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
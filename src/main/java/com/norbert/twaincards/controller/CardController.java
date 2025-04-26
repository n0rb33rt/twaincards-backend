package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.service.CardService;
import com.norbert.twaincards.service.UserActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контролер для управління картками
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

  private final CardService cardService;
  private final UserActivityLogService activityLogService;

  /**
   * Отримати картки колекції
   */
  @GetMapping("/collection/{collectionId}")
  public ResponseEntity<List<CardDTO>> getCardsByCollection(
          @PathVariable Long collectionId,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to get cards for collection with id: {}", collectionId);
    return ResponseEntity.ok(cardService.getCardsByCollection(collectionId, userId));
  }

  /**
   * Отримати сторінку карток колекції
   */
  @GetMapping("/collection/{collectionId}/page")
  public ResponseEntity<Page<CardDTO>> getCardsByCollection(
          @PathVariable Long collectionId,
          @RequestAttribute("userId") Long userId,
          Pageable pageable) {

    log.info("Request to get cards page for collection with id: {}", collectionId);
    return ResponseEntity.ok(cardService.getCardsByCollection(collectionId, userId, pageable));
  }

  /**
   * Отримати картку за ідентифікатором
   */
  @GetMapping("/{id}")
  public ResponseEntity<CardDTO> getCardById(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to get card by id: {}", id);
    return ResponseEntity.ok(cardService.getCardById(id, userId));
  }

  /**
   * Створити нову картку
   */
  @PostMapping
  public ResponseEntity<CardDTO> createCard(
          @RequestAttribute("userId") Long userId,
          @RequestBody @Valid CardDTO.CreateCardRequest createCardRequest) {

    log.info("Request to create new card for collection with id: {}", createCardRequest.getCollectionId());
    CardDTO createdCard = cardService.createCard(userId, createCardRequest);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "CREATE",
            "CARD",
            createdCard.getId(),
            "Card created with front text: " + createdCard.getFrontText()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
  }

  /**
   * Оновити картку
   */
  @PutMapping("/{id}")
  public ResponseEntity<CardDTO> updateCard(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId,
          @RequestBody @Valid CardDTO cardDTO) {

    log.info("Request to update card with id: {}", id);

    // Перевірка прав доступу буде виконана у сервісі
    CardDTO updatedCard = cardService.updateCard(id, userId, cardDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "UPDATE",
            "CARD",
            id,
            "Card updated with front text: " + updatedCard.getFrontText()
    );

    return ResponseEntity.ok(updatedCard);
  }

  /**
   * Видалити картку
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCard(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to delete card with id: {}", id);

    // Запам'ятовуємо front text для логів
    String frontText = cardService.getCardById(id, userId).getFrontText();

    // Перевірка прав доступу буде виконана у сервісі
    cardService.deleteCard(id, userId);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "DELETE",
            "CARD",
            id,
            "Card deleted with front text: " + frontText
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Пошук карток за текстом
   */
  @GetMapping("/search")
  public ResponseEntity<List<CardDTO>> searchCards(
          @RequestParam Long collectionId,
          @RequestParam String query,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to search cards in collection with id: {} and query: {}", collectionId, query);
    return ResponseEntity.ok(cardService.searchCards(collectionId, userId, query));
  }

  /**
   * Отримати картки за тегом
   */
  @GetMapping("/by-tag")
  public ResponseEntity<List<CardDTO>> getCardsByTag(
          @RequestParam Long collectionId,
          @RequestParam String tagName,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to get cards by tag: {} in collection with id: {}", tagName, collectionId);
    return ResponseEntity.ok(cardService.getCardsByTag(collectionId, userId, tagName));
  }

  /**
   * Додати теги до картки
   */
  @PostMapping("/{id}/tags")
  public ResponseEntity<CardDTO> addTagsToCard(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId,
          @RequestBody List<String> tagNames) {

    log.info("Request to add tags to card with id: {}", id);
    CardDTO updatedCard = cardService.addTagsToCard(id, userId, tagNames);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "ADD_TAGS",
            "CARD",
            id,
            "Tags added to card: " + String.join(", ", tagNames)
    );

    return ResponseEntity.ok(updatedCard);
  }

  /**
   * Видалити тег з картки
   */
  @DeleteMapping("/{id}/tags/{tagName}")
  public ResponseEntity<CardDTO> removeTagFromCard(
          @PathVariable Long id,
          @PathVariable String tagName,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to remove tag: {} from card with id: {}", tagName, id);
    CardDTO updatedCard = cardService.removeTagFromCard(id, userId, tagName);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "REMOVE_TAG",
            "CARD",
            id,
            "Tag removed from card: " + tagName
    );

    return ResponseEntity.ok(updatedCard);
  }
}
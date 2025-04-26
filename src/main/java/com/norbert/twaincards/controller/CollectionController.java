package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.service.CollectionService;
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
 * Контролер для управління колекціями карток
 */
@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

  private final CollectionService collectionService;
  private final UserActivityLogService activityLogService;

  /**
   * Отримати всі колекції користувача
   */
  @GetMapping("/user")
  public ResponseEntity<List<CollectionDTO>> getUserCollections(@RequestAttribute("userId") Long userId) {
    log.info("Request to get collections for user with id: {}", userId);
    return ResponseEntity.ok(collectionService.getUserCollections(userId));
  }

  /**
   * Отримати сторінку колекцій користувача
   */
  @GetMapping("/user/page")
  public ResponseEntity<Page<CollectionDTO>> getUserCollections(
          @RequestAttribute("userId") Long userId,
          Pageable pageable) {

    log.info("Request to get collections page for user with id: {}", userId);
    return ResponseEntity.ok(collectionService.getUserCollections(userId, pageable));
  }

  /**
   * Отримати колекцію за ідентифікатором
   */
  @GetMapping("/{id}")
  public ResponseEntity<CollectionDTO> getCollectionById(@PathVariable Long id) {
    log.info("Request to get collection by id: {}", id);
    return ResponseEntity.ok(collectionService.getCollectionById(id));
  }

  /**
   * Отримати публічні колекції
   */
  @GetMapping("/public")
  public ResponseEntity<Page<CollectionDTO>> getPublicCollections(Pageable pageable) {
    log.info("Request to get public collections page");
    return ResponseEntity.ok(collectionService.getPublicCollections(pageable));
  }

  /**
   * Пошук публічних колекцій за текстом
   */
  @GetMapping("/public/search")
  public ResponseEntity<Page<CollectionDTO>> searchPublicCollections(
          @RequestParam String query,
          Pageable pageable) {

    log.info("Request to search public collections with query: {}", query);
    return ResponseEntity.ok(collectionService.searchPublicCollections(query, pageable));
  }

  /**
   * Пошук колекцій користувача за текстом
   */
  @GetMapping("/user/search")
  public ResponseEntity<Page<CollectionDTO>> searchUserCollections(
          @RequestAttribute("userId") Long userId,
          @RequestParam String query,
          Pageable pageable) {

    log.info("Request to search collections for user with id: {} and query: {}", userId, query);
    return ResponseEntity.ok(collectionService.searchUserCollections(userId, query, pageable));
  }

  /**
   * Створити нову колекцію
   */
  @PostMapping
  public ResponseEntity<CollectionDTO> createCollection(
          @RequestAttribute("userId") Long userId,
          @RequestBody @Valid CollectionDTO collectionDTO) {

    log.info("Request to create new collection for user with id: {}", userId);
    CollectionDTO createdCollection = collectionService.createCollection(userId, collectionDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "CREATE",
            "COLLECTION",
            createdCollection.getId(),
            "Collection created: " + createdCollection.getName()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(createdCollection);
  }

  /**
   * Оновити колекцію
   */
  @PutMapping("/{id}")
  public ResponseEntity<CollectionDTO> updateCollection(
          @PathVariable Long id,
          @RequestBody @Valid CollectionDTO collectionDTO,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to update collection with id: {}", id);

    // Перевірка прав доступу буде виконана у сервісі
    CollectionDTO updatedCollection = collectionService.updateCollection(id, collectionDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "UPDATE",
            "COLLECTION",
            id,
            "Collection updated: " + updatedCollection.getName()
    );

    return ResponseEntity.ok(updatedCollection);
  }

  /**
   * Змінити публічність колекції
   */
  @PostMapping("/{id}/public/{isPublic}")
  public ResponseEntity<CollectionDTO> updateCollectionPublicStatus(
          @PathVariable Long id,
          @PathVariable Boolean isPublic,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to update collection public status with id: {} to: {}", id, isPublic);

    // Перевірка прав доступу буде виконана у сервісі
    CollectionDTO updatedCollection = collectionService.updateCollectionPublicStatus(id, isPublic);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "UPDATE",
            "COLLECTION",
            id,
            "Collection public status updated to: " + isPublic
    );

    return ResponseEntity.ok(updatedCollection);
  }

  /**
   * Видалити колекцію
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCollection(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to delete collection with id: {}", id);

    // Збереження назви колекції для логування перед видаленням
    String collectionName = collectionService.getCollectionById(id).getName();

    // Перевірка прав доступу буде виконана у сервісі
    collectionService.deleteCollection(id);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "DELETE",
            "COLLECTION",
            id,
            "Collection deleted: " + collectionName
    );

    return ResponseEntity.ok().build();
  }
}
package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.TagDTO;
import com.norbert.twaincards.service.TagService;
import com.norbert.twaincards.service.UserActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контролер для управління тегами
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {

  private final TagService tagService;
  private final UserActivityLogService activityLogService;

  /**
   * Отримати всі теги
   */
  @GetMapping
  public ResponseEntity<List<TagDTO>> getAllTags() {
    log.info("Request to get all tags");
    return ResponseEntity.ok(tagService.getAllTags());
  }

  /**
   * Отримати тег за ідентифікатором
   */
  @GetMapping("/{id}")
  public ResponseEntity<TagDTO> getTagById(@PathVariable Long id) {
    log.info("Request to get tag by id: {}", id);
    return ResponseEntity.ok(tagService.getTagById(id));
  }

  /**
   * Отримати тег за назвою
   */
  @GetMapping("/name/{name}")
  public ResponseEntity<TagDTO> getTagByName(@PathVariable String name) {
    log.info("Request to get tag by name: {}", name);
    return ResponseEntity.ok(tagService.getTagByName(name));
  }

  /**
   * Пошук тегів за частковою назвою
   */
  @GetMapping("/search")
  public ResponseEntity<List<TagDTO>> searchTags(@RequestParam String query) {
    log.info("Request to search tags with query: {}", query);
    return ResponseEntity.ok(tagService.searchTags(query));
  }

  /**
   * Отримати теги колекції
   */
  @GetMapping("/collection/{collectionId}")
  public ResponseEntity<List<TagDTO>> getTagsByCollection(@PathVariable Long collectionId) {
    log.info("Request to get tags for collection with id: {}", collectionId);
    return ResponseEntity.ok(tagService.getTagsByCollection(collectionId));
  }

  /**
   * Отримати найпопулярніші теги
   */
  @GetMapping("/popular")
  public ResponseEntity<List<TagDTO>> getMostPopularTags(@RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get {} most popular tags", limit);
    return ResponseEntity.ok(tagService.getMostPopularTags(limit));
  }

  /**
   * Створити новий тег
   */
  @PostMapping
  public ResponseEntity<TagDTO> createTag(
          @RequestBody @Valid TagDTO tagDTO,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to create new tag with name: {}", tagDTO.getName());
    TagDTO createdTag = tagService.createTag(tagDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "CREATE",
            "TAG",
            createdTag.getId(),
            "Tag created: " + createdTag.getName()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
  }

  /**
   * Оновити тег
   */
  @PutMapping("/{id}")
  public ResponseEntity<TagDTO> updateTag(
          @PathVariable Long id,
          @RequestBody @Valid TagDTO tagDTO,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to update tag with id: {}", id);
    TagDTO updatedTag = tagService.updateTag(id, tagDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "UPDATE",
            "TAG",
            id,
            "Tag updated: " + updatedTag.getName()
    );

    return ResponseEntity.ok(updatedTag);
  }

  /**
   * Видалити тег
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTag(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to delete tag with id: {}", id);

    // Запам'ятовуємо назву тегу для логування
    String tagName = tagService.getTagById(id).getName();

    tagService.deleteTag(id);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "DELETE",
            "TAG",
            id,
            "Tag deleted: " + tagName
    );

    return ResponseEntity.ok().build();
  }
}
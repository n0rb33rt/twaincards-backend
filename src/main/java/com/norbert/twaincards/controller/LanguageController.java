package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.LanguageDTO;
import com.norbert.twaincards.service.LanguageService;
import com.norbert.twaincards.service.UserActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контролер для управління мовами
 */
@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
@Slf4j
public class LanguageController {

  private final LanguageService languageService;
  private final UserActivityLogService activityLogService;

  /**
   * Отримати всі мови
   */
  @GetMapping
  public ResponseEntity<List<LanguageDTO>> getAllLanguages() {
    log.info("Request to get all languages");
    return ResponseEntity.ok(languageService.getAllLanguages());
  }

  /**
   * Отримати всі активні мови
   */
  @GetMapping("/enabled")
  public ResponseEntity<List<LanguageDTO>> getAllEnabledLanguages() {
    log.info("Request to get all enabled languages");
    return ResponseEntity.ok(languageService.getAllEnabledLanguages());
  }

  /**
   * Отримати мову за ідентифікатором
   */
  @GetMapping("/{id}")
  public ResponseEntity<LanguageDTO> getLanguageById(@PathVariable Long id) {
    log.info("Request to get language by id: {}", id);
    return ResponseEntity.ok(languageService.getLanguageById(id));
  }

  /**
   * Отримати мову за кодом
   */
  @GetMapping("/code/{code}")
  public ResponseEntity<LanguageDTO> getLanguageByCode(@PathVariable String code) {
    log.info("Request to get language by code: {}", code);
    return ResponseEntity.ok(languageService.getLanguageByCode(code));
  }

  /**
   * Пошук мов за назвою або кодом
   */
  @GetMapping("/search")
  public ResponseEntity<List<LanguageDTO>> searchLanguages(@RequestParam String query) {
    log.info("Request to search languages with query: {}", query);
    return ResponseEntity.ok(languageService.searchLanguages(query));
  }

  /**
   * Створити нову мову (лише для адміністраторів)
   */
  @PostMapping
  public ResponseEntity<LanguageDTO> createLanguage(
          @RequestBody @Valid LanguageDTO languageDTO,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to create new language with code: {}", languageDTO.getCode());
    LanguageDTO createdLanguage = languageService.createLanguage(languageDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "CREATE",
            "LANGUAGE",
            createdLanguage.getId(),
            "Language created: " + createdLanguage.getCode()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(createdLanguage);
  }

  /**
   * Оновити мову (лише для адміністраторів)
   */
  @PutMapping("/{id}")
  public ResponseEntity<LanguageDTO> updateLanguage(
          @PathVariable Long id,
          @RequestBody @Valid LanguageDTO languageDTO,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to update language with id: {}", id);
    LanguageDTO updatedLanguage = languageService.updateLanguage(id, languageDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "UPDATE",
            "LANGUAGE",
            id,
            "Language updated: " + updatedLanguage.getCode()
    );

    return ResponseEntity.ok(updatedLanguage);
  }

  /**
   * Активувати мову (лише для адміністраторів)
   */
  @PostMapping("/{id}/enable")
  public ResponseEntity<Void> enableLanguage(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to enable language with id: {}", id);
    languageService.enableLanguage(id);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "ENABLE",
            "LANGUAGE",
            id,
            "Language enabled"
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Деактивувати мову (лише для адміністраторів)
   */
  @PostMapping("/{id}/disable")
  public ResponseEntity<Void> disableLanguage(
          @PathVariable Long id,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to disable language with id: {}", id);
    languageService.disableLanguage(id);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "DISABLE",
            "LANGUAGE",
            id,
            "Language disabled"
    );

    return ResponseEntity.ok().build();
  }
}
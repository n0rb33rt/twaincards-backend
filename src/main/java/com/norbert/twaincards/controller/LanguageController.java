package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.LanguageDTO;
import com.norbert.twaincards.service.LanguageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
@Slf4j
public class LanguageController {

  private final LanguageService languageService;


  @GetMapping
  public ResponseEntity<List<LanguageDTO>> getAllLanguages() {
    log.info("Request to get all languages");
    return ResponseEntity.ok(languageService.getAllLanguages());
  }


  @GetMapping("/{id}")
  public ResponseEntity<LanguageDTO> getLanguageById(@PathVariable Long id) {
    log.info("Request to get language by id: {}", id);
    return ResponseEntity.ok(languageService.getLanguageById(id));
  }

  @GetMapping("/code/{code}")
  public ResponseEntity<LanguageDTO> getLanguageByCode(@PathVariable String code) {
    log.info("Request to get language by code: {}", code);
    return ResponseEntity.ok(languageService.getLanguageByCode(code));
  }


  @GetMapping("/search")
  public ResponseEntity<List<LanguageDTO>> searchLanguages(@RequestParam String query) {
    log.info("Request to search languages with query: {}", query);
    return ResponseEntity.ok(languageService.searchLanguages(query));
  }


  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<LanguageDTO> createLanguage(@Valid @RequestBody LanguageDTO languageDTO) {
    log.info("Request to create new language: {}", languageDTO.getCode());
    LanguageDTO newLanguage = languageService.createLanguage(languageDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(newLanguage);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<LanguageDTO> updateLanguage(
          @PathVariable Long id,
          @Valid @RequestBody LanguageDTO languageDTO) {

    log.info("Request to update language with id: {}", id);
    LanguageDTO updatedLanguage = languageService.updateLanguage(id, languageDTO);
    return ResponseEntity.ok(updatedLanguage);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteLanguage(@PathVariable Long id) {
    log.info("Request to delete language with id: {}", id);
    languageService.deleteLanguage(id);
    return ResponseEntity.noContent().build();
  }
}
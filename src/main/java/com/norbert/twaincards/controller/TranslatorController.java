package com.norbert.twaincards.controller;

import com.norbert.twaincards.model.request.AzureTranslatorRequest;
import com.norbert.twaincards.model.response.TranslatorResponse;
import com.norbert.twaincards.service.LanguageService;
import com.norbert.twaincards.service.TranslatorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translate")
@AllArgsConstructor
@Slf4j
public class TranslatorController {
  private final TranslatorService translatorService;

  @PostMapping
  public ResponseEntity<TranslatorResponse> translate(@RequestBody @Valid AzureTranslatorRequest request){
    log.info("Translate request received: from={}, to={}", request.fromLanguage(), request.toLanguage());
    return ResponseEntity.ok(translatorService.translate(request));
  }
}
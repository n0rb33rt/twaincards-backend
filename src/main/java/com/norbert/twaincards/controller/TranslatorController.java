package com.norbert.twaincards.controller;

import com.norbert.twaincards.model.request.AzureTranslatorRequest;
import com.norbert.twaincards.model.response.TranslatorResponse;
import com.norbert.twaincards.service.TranslatorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/translate")
@AllArgsConstructor
public class TranslatorController {
  private final TranslatorService translatorService;

  @PostMapping
  public ResponseEntity<TranslatorResponse> translate(@RequestBody @Valid AzureTranslatorRequest request){
    return ResponseEntity.ok(translatorService.translate(request));
  }
}
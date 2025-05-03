package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.entity.enumeration.ImportFormat;
import com.norbert.twaincards.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

  private final ImportService importService;

  @PostMapping("/collection/csv")
  public ResponseEntity<CollectionDTO> importCollectionFromCsv(
          @RequestParam("file") MultipartFile file) {
    log.info("Request to import collection from CSV file: {}", file.getOriginalFilename());
    CollectionDTO imported = importService.importCollection(file, ImportFormat.CSV);
    return ResponseEntity.status(HttpStatus.CREATED).body(imported);
  }

  @PostMapping("/collection/json")
  public ResponseEntity<CollectionDTO> importCollectionFromJson(
          @RequestParam("file") MultipartFile file) {
    log.info("Request to import collection from JSON file: {}", file.getOriginalFilename());
    CollectionDTO imported = importService.importCollection(file, ImportFormat.JSON);
    return ResponseEntity.status(HttpStatus.CREATED).body(imported);
  }

  @PostMapping("/collection/xml")
  public ResponseEntity<CollectionDTO> importCollectionFromXml(
          @RequestParam("file") MultipartFile file) {
    log.info("Request to import collection from XML file: {}", file.getOriginalFilename());
    CollectionDTO imported = importService.importCollection(file, ImportFormat.XML);
    return ResponseEntity.status(HttpStatus.CREATED).body(imported);
  }
}
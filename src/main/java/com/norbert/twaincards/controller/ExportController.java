package com.norbert.twaincards.controller;

import com.norbert.twaincards.entity.enumeration.ExportFormat;
import com.norbert.twaincards.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

  private final ExportService exportService;

  @GetMapping("/collection/{collectionId}/csv")
  public ResponseEntity<String> exportCollectionToCsv(
          @PathVariable Long collectionId) {
    log.info("Request to export collection with id: {} to CSV", collectionId);
    String result = exportService.exportCollection(collectionId, ExportFormat.CSV);
    return createExportResponse(result, "collection_" + collectionId + ".csv", "text/csv");
  }

  @GetMapping("/collection/{collectionId}/json")
  public ResponseEntity<String> exportCollectionToJson(
          @PathVariable Long collectionId) {
    log.info("Request to export collection with id: {} to JSON", collectionId);
    String result = exportService.exportCollection(collectionId, ExportFormat.JSON);
    return createExportResponse(result, "collection_" + collectionId + ".json", "application/json");
  }

  @GetMapping("/collection/{collectionId}/xml")
  public ResponseEntity<String> exportCollectionToXml(
          @PathVariable Long collectionId) {
    log.info("Request to export collection with id: {} to XML", collectionId);
    String result = exportService.exportCollection(collectionId, ExportFormat.XML);
    return createExportResponse(result, "collection_" + collectionId + ".xml", "application/xml");
  }

  private ResponseEntity<String> createExportResponse(String content, String filename, String contentType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(contentType));
    headers.setContentDispositionFormData("attachment", filename);

    return ResponseEntity.ok()
            .headers(headers)
            .body(content);
  }
}
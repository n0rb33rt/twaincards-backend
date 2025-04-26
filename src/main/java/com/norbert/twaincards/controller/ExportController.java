package com.norbert.twaincards.controller;

import com.norbert.twaincards.service.ExportService;
import com.norbert.twaincards.service.ExportService.ExportFormat;
import com.norbert.twaincards.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контролер для експорту даних
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

  private final ExportService exportService;
  private final UserActivityLogService activityLogService;

  /**
   * Експортувати колекцію у форматі CSV
   */
  @GetMapping("/collection/{collectionId}/csv")
  public ResponseEntity<String> exportCollectionToCsv(
          @PathVariable Long collectionId,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to export collection with id: {} to CSV", collectionId);
    String result = exportService.exportCollection(userId, collectionId, ExportFormat.CSV);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "EXPORT",
            "COLLECTION",
            collectionId,
            "Collection exported to CSV"
    );

    return createExportResponse(result, "collection_" + collectionId + ".csv", "text/csv");
  }

  /**
   * Експортувати колекцію у форматі JSON
   */
  @GetMapping("/collection/{collectionId}/json")
  public ResponseEntity<String> exportCollectionToJson(
          @PathVariable Long collectionId,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to export collection with id: {} to JSON", collectionId);
    String result = exportService.exportCollection(userId, collectionId, ExportFormat.JSON);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "EXPORT",
            "COLLECTION",
            collectionId,
            "Collection exported to JSON"
    );

    return createExportResponse(result, "collection_" + collectionId + ".json", "application/json");
  }

  /**
   * Експортувати колекцію у форматі XML
   */
  @GetMapping("/collection/{collectionId}/xml")
  public ResponseEntity<String> exportCollectionToXml(
          @PathVariable Long collectionId,
          @RequestAttribute("userId") Long userId) {

    log.info("Request to export collection with id: {} to XML", collectionId);
    String result = exportService.exportCollection(userId, collectionId, ExportFormat.XML);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "EXPORT",
            "COLLECTION",
            collectionId,
            "Collection exported to XML"
    );

    return createExportResponse(result, "collection_" + collectionId + ".xml", "application/xml");
  }

  /**
   * Створити відповідь для експорту файлу
   */
  private ResponseEntity<String> createExportResponse(String content, String filename, String contentType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(contentType));
    headers.setContentDispositionFormData("attachment", filename);

    return ResponseEntity.ok()
            .headers(headers)
            .body(content);
  }
}
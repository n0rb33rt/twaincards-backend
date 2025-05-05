package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.enumeration.ExportFormat;
import com.norbert.twaincards.service.CollectionService;
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

import java.util.Optional;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

  private final ExportService exportService;
  private final CollectionService collectionService;

  @GetMapping("/collection/{collectionId}/csv")
  public ResponseEntity<byte[]> exportCollectionToCsv(
          @PathVariable Long collectionId) {
    log.info("Request to export collection with id: {} to CSV", collectionId);
    String result = exportService.exportCollection(collectionId, ExportFormat.CSV);
    CollectionDTO collection = collectionService.getCollectionById(collectionId);
    return createExportResponse(result.getBytes(), collection.getName() + "_exported" + ".csv", "text/csv");
  }

  private ResponseEntity<byte[]> createExportResponse(byte[] content, String filename, String contentType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(contentType));
    headers.setContentDispositionFormData("attachment", filename);
    headers.setCacheControl("no-cache, no-store, must-revalidate");
    headers.setPragma("no-cache");
    headers.setExpires(0);

    return ResponseEntity.ok()
            .headers(headers)
            .body(content);
  }
}
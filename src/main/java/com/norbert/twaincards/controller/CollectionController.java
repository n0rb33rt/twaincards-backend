package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

  private final CollectionService collectionService;

  @GetMapping("/user")
  public ResponseEntity<List<CollectionDTO>> getUserCollections() {
    return ResponseEntity.ok(collectionService.getAllCollectionsByUser());
  }

  @GetMapping("/user/page")
  public ResponseEntity<Page<CollectionDTO>> getAllCollections(Pageable pageable) {
    log.info("Request to get collections page");
    return ResponseEntity.ok(collectionService.getUserCollections(pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<CollectionDTO> getCollectionById(@PathVariable Long id) {
    log.info("Request to get collection by id: {}", id);
    return ResponseEntity.ok(collectionService.getCollectionById(id));
  }

  @GetMapping("/public")
  public ResponseEntity<Page<CollectionDTO>> getPublicCollections(Pageable pageable) {
    log.info("Request to get public collections page");
    return ResponseEntity.ok(collectionService.getPublicCollections(pageable));
  }

  @GetMapping("/public/search")
  public ResponseEntity<Page<CollectionDTO>> searchPublicCollections(
          @RequestParam String query,
          Pageable pageable) {
    log.info("Request to search public collections with query: {}", query);
    return ResponseEntity.ok(collectionService.searchPublicCollections(query, pageable));
  }

  @GetMapping("/user/search")
  public ResponseEntity<Page<CollectionDTO>> searchUserCollections(
          @RequestParam String query,
          Pageable pageable) {
    log.info("Request to search user collections with query: {}", query);
    return ResponseEntity.ok(collectionService.searchUserCollections(query, pageable));
  }

  @GetMapping("/user/recent")
  public ResponseEntity<List<CollectionDTO>> getRecentCollections(@RequestParam(defaultValue = "5") int limit) {
    log.info("Request to get recent collections with limit: {}", limit);
    return ResponseEntity.ok(collectionService.getRecentCollectionsByUser(limit));
  }

  @PostMapping
  public ResponseEntity<CollectionDTO> createCollection(@RequestBody @Valid CollectionDTO collectionDTO) {
    log.info("Request to create new collection");
    CollectionDTO createdCollection = collectionService.createCollection(collectionDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdCollection);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CollectionDTO> updateCollection(
          @PathVariable Long id,
          @RequestBody @Valid CollectionDTO collectionDTO) {
    log.info("Request to update collection with id: {}", id);
    CollectionDTO updatedCollection = collectionService.updateCollection(id, collectionDTO);
    return ResponseEntity.ok(updatedCollection);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCollection(@PathVariable Long id) {
    log.info("Request to delete collection with id: {}", id);
    collectionService.deleteCollection(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/users-count")
  public ResponseEntity<Integer> getCollectionUsersCount(@PathVariable Long id) {
    log.info("Request to get the number of users for collection: {}", id);
    Integer usersCount = collectionService.getCollectionUsersCount(id);
    return ResponseEntity.ok(usersCount);
  }
}
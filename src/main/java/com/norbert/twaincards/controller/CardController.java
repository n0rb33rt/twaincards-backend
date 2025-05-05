package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.service.CardService;
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
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

  private final CardService cardService;

  @GetMapping("/collection/{collectionId}")
  public ResponseEntity<List<CardDTO>> getCardsByCollection(
          @PathVariable Long collectionId) {
    log.info("Request to get cards for collection with id: {}", collectionId);
    return ResponseEntity.ok(cardService.getCardsByCollection(collectionId));
  }

  @GetMapping("/collection/{collectionId}/page")
  public ResponseEntity<Page<CardDTO>> getCardsByCollection(
          @PathVariable Long collectionId,
          Pageable pageable) {
    log.info("Request to get cards page for collection with id: {}", collectionId);
    return ResponseEntity.ok(cardService.getCardsByCollection(collectionId, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<CardDTO> getCardById(
          @PathVariable Long id) {
    log.info("Request to get card by id: {}", id);
    return ResponseEntity.ok(cardService.getCardById(id));
  }

  @PostMapping
  public ResponseEntity<CardDTO> createCard(
          @RequestBody @Valid CardDTO.CreateCardRequest createCardRequest) {
    log.info("Request to create new card for collection with id: {}", createCardRequest.getCollectionId());
    CardDTO createdCard = cardService.createCard(createCardRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CardDTO> updateCard(
          @PathVariable Long id,
          @RequestBody @Valid CardDTO cardDTO) {
    log.info("Request to update card with id: {}", id);
    CardDTO updatedCard = cardService.updateCard(id, cardDTO);
    return ResponseEntity.ok(updatedCard);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCard(
          @PathVariable Long id) {
    log.info("Request to delete card with id: {}", id);
    cardService.deleteCard(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/search")
  public ResponseEntity<List<CardDTO>> searchCards(
          @RequestParam Long collectionId,
          @RequestParam String query) {
    log.info("Request to search cards in collection with id: {} and query: {}", collectionId, query);
    return ResponseEntity.ok(cardService.searchCards(collectionId, query));
  }

}
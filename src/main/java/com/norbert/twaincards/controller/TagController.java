package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.TagDTO;
import com.norbert.twaincards.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {

  private final TagService tagService;

  @GetMapping
  public ResponseEntity<List<TagDTO>> getAllTags() {
    log.info("Request to get all tags");
    return ResponseEntity.ok(tagService.getAllTags());
  }

  @GetMapping("/{id}")
  public ResponseEntity<TagDTO> getTagById(@PathVariable Long id) {
    log.info("Request to get tag by id: {}", id);
    return ResponseEntity.ok(tagService.getTagById(id));
  }

  @GetMapping("/name/{name}")
  public ResponseEntity<TagDTO> getTagByName(@PathVariable String name) {
    log.info("Request to get tag by name: {}", name);
    return ResponseEntity.ok(tagService.getTagByName(name));
  }

  @GetMapping("/search")
  public ResponseEntity<List<TagDTO>> searchTags(@RequestParam String query) {
    log.info("Request to search tags with query: {}", query);
    return ResponseEntity.ok(tagService.searchTags(query));
  }

  @GetMapping("/popular")
  public ResponseEntity<List<TagDTO>> getMostPopularTags(@RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get {} most popular tags", limit);
    return ResponseEntity.ok(tagService.getMostPopularTags(limit));
  }

  @PostMapping
  public ResponseEntity<TagDTO> createTag(@RequestBody @Valid TagDTO tagDTO) {
    log.info("Request to create new tag with name: {}", tagDTO.getName());
    TagDTO createdTag = tagService.createTag(tagDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
  }

  @PutMapping("/{id}")
  public ResponseEntity<TagDTO> updateTag(
          @PathVariable Long id,
          @RequestBody @Valid TagDTO tagDTO) {
    log.info("Request to update tag with id: {}", id);
    TagDTO updatedTag = tagService.updateTag(id, tagDTO);
    return ResponseEntity.ok(updatedTag);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
    log.info("Request to delete tag with id: {}", id);
    tagService.deleteTag(id);
    return ResponseEntity.ok().build();
  }
}
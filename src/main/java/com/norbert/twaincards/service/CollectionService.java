package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.Language;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.AccessDeniedException;
import com.norbert.twaincards.exception.ResourceAlreadyExistsException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.LanguageRepository;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

  private final CollectionRepository collectionRepository;
  private final LanguageRepository languageRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @Transactional(readOnly = true)
  public List<CollectionDTO> getAllCollectionsByUser() {
    User user = securityUtils.getCurrentUser();
    return collectionRepository.findByUser(user).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<CollectionDTO> getUserCollections(Pageable pageable) {
    User user = securityUtils.getCurrentUser();
    log.debug("Getting collections page for user: {}", user.getUsername());

    Page<Collection> collectionsPage = collectionRepository.findByUser(user, pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public CollectionDTO getCollectionById(Long id) {
    User currentUser = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findByIdWithLanguages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You don't have permission to access this collection");
    }
    return convertToDto(collection);
  }

  @Transactional(readOnly = true)
  public Page<CollectionDTO> getPublicCollections(Pageable pageable) {
    Page<Collection> collectionsPage = collectionRepository.findByIsPublicTrue(pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Page<CollectionDTO> searchPublicCollections(String searchText, Pageable pageable) {
    Page<Collection> collectionsPage = collectionRepository.searchPublicCollections(searchText, pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Page<CollectionDTO> searchUserCollections(String searchText, Pageable pageable) {
    User user = securityUtils.getCurrentUser();
    Page<Collection> collectionsPage = collectionRepository.searchUserCollections(user, searchText, pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  @Transactional
  public CollectionDTO createCollection(CollectionDTO collectionDTO) {
    User user = securityUtils.getCurrentUser();

    if (collectionRepository.findByUserAndName(user, collectionDTO.getName()).isPresent()) {
      throw new ResourceAlreadyExistsException("Collection with name already exists: " + collectionDTO.getName());
    }

    Language sourceLanguage = languageRepository.findById(collectionDTO.getSourceLanguageId())
            .orElseThrow(() -> new ResourceNotFoundException("Source language not found with id: " + collectionDTO.getSourceLanguageId()));

    Language targetLanguage = languageRepository.findById(collectionDTO.getTargetLanguageId())
            .orElseThrow(() -> new ResourceNotFoundException("Target language not found with id: " + collectionDTO.getTargetLanguageId()));

    Collection collection = Collection.builder()
            .user(user)
            .name(collectionDTO.getName())
            .description(collectionDTO.getDescription())
            .sourceLanguage(sourceLanguage)
            .targetLanguage(targetLanguage)
            .isPublic(collectionDTO.getIsPublic() != null ? collectionDTO.getIsPublic() : false)
            .build();

    Collection savedCollection = collectionRepository.save(collection);
    log.info("Collection created successfully with id: {}", savedCollection.getId());

    return convertToDto(savedCollection);
  }

  @Transactional
  public CollectionDTO updateCollection(Long id, CollectionDTO collectionDTO) {
    User currentUser = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

    if (!collection.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You don't have permission to update this collection");
    }

    collection.setName(collectionDTO.getName());
    collection.setDescription(collectionDTO.getDescription());

    if (collectionDTO.getIsPublic() != null) {
      collection.setIsPublic(collectionDTO.getIsPublic());
    }

    if (collectionDTO.getSourceLanguageId() != null) {
      Language sourceLanguage = languageRepository.findById(collectionDTO.getSourceLanguageId())
              .orElseThrow(() -> new ResourceNotFoundException("Source language not found with id: " + collectionDTO.getSourceLanguageId()));
      collection.setSourceLanguage(sourceLanguage);
    }

    if (collectionDTO.getTargetLanguageId() != null) {
      Language targetLanguage = languageRepository.findById(collectionDTO.getTargetLanguageId())
              .orElseThrow(() -> new ResourceNotFoundException("Target language not found with id: " + collectionDTO.getTargetLanguageId()));
      collection.setTargetLanguage(targetLanguage);
    }

    collection.setUpdatedAt(LocalDateTime.now());
    Collection updatedCollection = collectionRepository.save(collection);
    log.info("Collection updated successfully with id: {}", updatedCollection.getId());

    return convertToDto(updatedCollection);
  }

  @Transactional
  public void deleteCollection(Long id) {
    User currentUser = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

    if (!collection.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You don't have permission to delete this collection");
    }

    collectionRepository.delete(collection);
    log.info("Collection deleted successfully with id: {}", id);
  }

  private CollectionDTO convertToDto(Collection collection) {
    CollectionDTO collectionDTO = modelMapper.map(collection, CollectionDTO.class);

    if (collection.getUser() != null) {
      collectionDTO.setUserId(collection.getUser().getId());
      collectionDTO.setUsername(collection.getUser().getUsername());
    }

    if (collection.getSourceLanguage() != null) {
      collectionDTO.setSourceLanguageId(collection.getSourceLanguage().getId());
      collectionDTO.setSourceLanguageName(collection.getSourceLanguage().getName());
      collectionDTO.setSourceLanguageCode(collection.getSourceLanguage().getCode());
    }

    if (collection.getTargetLanguage() != null) {
      collectionDTO.setTargetLanguageId(collection.getTargetLanguage().getId());
      collectionDTO.setTargetLanguageName(collection.getTargetLanguage().getName());
      collectionDTO.setTargetLanguageCode(collection.getTargetLanguage().getCode());
    }

    collectionDTO.setCardCount(collection.getCards().size());

    return collectionDTO;
  }
}
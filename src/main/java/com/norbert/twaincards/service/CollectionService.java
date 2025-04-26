package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.Language;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.ResourceAlreadyExistsException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.LanguageRepository;
import com.norbert.twaincards.repository.LearningProgressRepository;
import com.norbert.twaincards.repository.UserRepository;
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

/**
 * Сервіс для роботи з колекціями карток
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

  private final CollectionRepository collectionRepository;
  private final UserRepository userRepository;
  private final LanguageRepository languageRepository;
  private final LearningProgressRepository learningProgressRepository;
  private final ModelMapper modelMapper;

  /**
   * Отримати всі колекції користувача
   * @param userId ідентифікатор користувача
   * @return список DTO колекцій
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public List<CollectionDTO> getUserCollections(Long userId) {
    log.debug("Getting collections for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    return collectionRepository.findByUser(user).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати сторінку колекцій користувача
   * @param userId ідентифікатор користувача
   * @param pageable параметри пагінації
   * @return сторінка DTO колекцій
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public Page<CollectionDTO> getUserCollections(Long userId, Pageable pageable) {
    log.debug("Getting collections page for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Page<Collection> collectionsPage = collectionRepository.findByUser(user, pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  /**
   * Отримати колекцію за ідентифікатором
   * @param id ідентифікатор колекції
   * @return DTO колекції
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   */
  @Transactional(readOnly = true)
  public CollectionDTO getCollectionById(Long id) {
    log.debug("Getting collection by id: {}", id);
    Collection collection = collectionRepository.findByIdWithLanguages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));
    return convertToDto(collection);
  }

  /**
   * Отримати публічні колекції
   * @param pageable параметри пагінації
   * @return сторінка DTO публічних колекцій
   */
  @Transactional(readOnly = true)
  public Page<CollectionDTO> getPublicCollections(Pageable pageable) {
    log.debug("Getting public collections page");
    Page<Collection> collectionsPage = collectionRepository.findByIsPublicTrue(pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  /**
   * Пошук публічних колекцій за текстом
   * @param searchText текст для пошуку
   * @param pageable параметри пагінації
   * @return сторінка DTO знайдених колекцій
   */
  @Transactional(readOnly = true)
  public Page<CollectionDTO> searchPublicCollections(String searchText, Pageable pageable) {
    log.debug("Searching public collections with text: {}", searchText);
    Page<Collection> collectionsPage = collectionRepository.searchPublicCollections(searchText, pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  /**
   * Пошук колекцій користувача за текстом
   * @param userId ідентифікатор користувача
   * @param searchText текст для пошуку
   * @param pageable параметри пагінації
   * @return сторінка DTO знайдених колекцій
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public Page<CollectionDTO> searchUserCollections(Long userId, String searchText, Pageable pageable) {
    log.debug("Searching collections for user with id: {} and text: {}", userId, searchText);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Page<Collection> collectionsPage = collectionRepository.searchUserCollections(user, searchText, pageable);
    List<CollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

    return new PageImpl<>(collectionDTOs, pageable, collectionsPage.getTotalElements());
  }

  /**
   * Створити нову колекцію
   * @param userId ідентифікатор користувача
   * @param collectionDTO дані колекції
   * @return DTO створеної колекції
   * @throws ResourceNotFoundException якщо користувача або мови не знайдено
   * @throws ResourceAlreadyExistsException якщо колекція з такою назвою вже існує у користувача
   */
  @Transactional
  public CollectionDTO createCollection(Long userId, CollectionDTO collectionDTO) {
    log.debug("Creating new collection for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // Перевірка існування колекції з такою назвою у користувача
    if (collectionRepository.findByUserAndName(user, collectionDTO.getName()).isPresent()) {
      throw new ResourceAlreadyExistsException("Collection with name already exists: " + collectionDTO.getName());
    }

    // Отримання мов
    Language sourceLanguage = languageRepository.findById(collectionDTO.getSourceLanguageId())
            .orElseThrow(() -> new ResourceNotFoundException("Source language not found with id: " + collectionDTO.getSourceLanguageId()));

    Language targetLanguage = languageRepository.findById(collectionDTO.getTargetLanguageId())
            .orElseThrow(() -> new ResourceNotFoundException("Target language not found with id: " + collectionDTO.getTargetLanguageId()));

    // Створення нової колекції
    Collection collection = Collection.builder()
            .user(user)
            .name(collectionDTO.getName())
            .description(collectionDTO.getDescription())
            .sourceLanguage(sourceLanguage)
            .targetLanguage(targetLanguage)
            .isPublic(collectionDTO.getIsPublic() != null ? collectionDTO.getIsPublic() : false)
            .isDefault(collectionDTO.getIsDefault() != null ? collectionDTO.getIsDefault() : false)
            .build();

    Collection savedCollection = collectionRepository.save(collection);
    log.info("Collection created successfully with id: {}", savedCollection.getId());

    return convertToDto(savedCollection);
  }

  /**
   * Оновити колекцію
   * @param id ідентифікатор колекції
   * @param collectionDTO нові дані колекції
   * @return DTO оновленої колекції
   * @throws ResourceNotFoundException якщо колекцію або мову не знайдено
   */
  @Transactional
  public CollectionDTO updateCollection(Long id, CollectionDTO collectionDTO) {
    log.debug("Updating collection with id: {}", id);
    Collection collection = collectionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

    // Оновлення основних даних
    collection.setName(collectionDTO.getName());
    collection.setDescription(collectionDTO.getDescription());

    if (collectionDTO.getIsPublic() != null) {
      collection.setIsPublic(collectionDTO.getIsPublic());
    }

    if (collectionDTO.getIsDefault() != null) {
      collection.setIsDefault(collectionDTO.getIsDefault());
    }

    // Оновлення мов, якщо вказані
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

  /**
   * Змінити публічність колекції
   * @param id ідентифікатор колекції
   * @param isPublic нове значення публічності
   * @return DTO оновленої колекції
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   */
  @Transactional
  public CollectionDTO updateCollectionPublicStatus(Long id, Boolean isPublic) {
    log.debug("Updating collection public status with id: {} to: {}", id, isPublic);
    Collection collection = collectionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

    collection.setIsPublic(isPublic);
    collection.setUpdatedAt(LocalDateTime.now());
    Collection updatedCollection = collectionRepository.save(collection);
    log.info("Collection public status updated successfully with id: {}", updatedCollection.getId());

    return convertToDto(updatedCollection);
  }

  /**
   * Видалити колекцію
   * @param id ідентифікатор колекції
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   */
  @Transactional
  public void deleteCollection(Long id) {
    log.debug("Deleting collection with id: {}", id);
    Collection collection = collectionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

    collectionRepository.delete(collection);
    log.info("Collection deleted successfully with id: {}", id);
  }

  /**
   * Конвертувати сутність колекції в DTO
   * @param collection сутність колекції
   * @return DTO колекції
   */
  private CollectionDTO convertToDto(Collection collection) {
    CollectionDTO collectionDTO = modelMapper.map(collection, CollectionDTO.class);

    // Додаткова інформація про користувача
    if (collection.getUser() != null) {
      collectionDTO.setUserId(collection.getUser().getId());
      collectionDTO.setUsername(collection.getUser().getUsername());
    }

    // Додаткова інформація про мови
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

    // Кількість карток в колекції
    collectionDTO.setCardCount(collection.getCards().size());

    return collectionDTO;
  }
}
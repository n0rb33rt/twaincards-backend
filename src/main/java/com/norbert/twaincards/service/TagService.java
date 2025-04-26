package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.TagDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.Tag;
import com.norbert.twaincards.exception.ResourceAlreadyExistsException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з тегами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

  private final TagRepository tagRepository;
  private final CollectionRepository collectionRepository;
  private final ModelMapper modelMapper;

  /**
   * Отримати всі теги
   * @return список DTO тегів
   */
  @Transactional(readOnly = true)
  public List<TagDTO> getAllTags() {
    log.debug("Getting all tags");
    return tagRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати тег за ідентифікатором
   * @param id ідентифікатор тегу
   * @return DTO тегу
   * @throws ResourceNotFoundException якщо тег не знайдено
   */
  @Transactional(readOnly = true)
  public TagDTO getTagById(Long id) {
    log.debug("Getting tag by id: {}", id);
    Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
    return convertToDto(tag);
  }

  /**
   * Отримати тег за назвою
   * @param name назва тегу
   * @return DTO тегу
   * @throws ResourceNotFoundException якщо тег не знайдено
   */
  @Transactional(readOnly = true)
  public TagDTO getTagByName(String name) {
    log.debug("Getting tag by name: {}", name);
    Tag tag = tagRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + name));
    return convertToDto(tag);
  }

  /**
   * Пошук тегів за частковою назвою
   * @param name частина назви тегу
   * @return список DTO знайдених тегів
   */
  @Transactional(readOnly = true)
  public List<TagDTO> searchTags(String name) {
    log.debug("Searching tags with name containing: {}", name);
    return tagRepository.findByNameContainingIgnoreCase(name).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати теги колекції
   * @param collectionId ідентифікатор колекції
   * @return список DTO тегів
   * @throws ResourceNotFoundException якщо колекцію не знайдено
   */
  @Transactional(readOnly = true)
  public List<TagDTO> getTagsByCollection(Long collectionId) {
    log.debug("Getting tags for collection with id: {}", collectionId);

    // Перевірка існування колекції
    if (!collectionRepository.existsById(collectionId)) {
      throw new ResourceNotFoundException("Collection not found with id: " + collectionId);
    }

    return tagRepository.findTagsByCollectionId(collectionId).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати найпопулярніші теги
   * @param limit кількість тегів
   * @return список DTO тегів з кількістю карток
   */
  @Transactional(readOnly = true)
  public List<TagDTO> getMostPopularTags(int limit) {
    log.debug("Getting {} most popular tags", limit);
    List<Object[]> popularTags = tagRepository.findMostPopularTags(PageRequest.of(0, limit));

    return popularTags.stream()
            .map(row -> {
              Tag tag = (Tag) row[0];
              Long cardCount = (Long) row[1];

              TagDTO tagDTO = convertToDto(tag);
              tagDTO.setCardCount(cardCount.intValue());
              return tagDTO;
            })
            .collect(Collectors.toList());
  }

  /**
   * Створити новий тег
   * @param tagDTO дані тегу
   * @return DTO створеного тегу
   * @throws ResourceAlreadyExistsException якщо тег з такою назвою вже існує
   */
  @Transactional
  public TagDTO createTag(TagDTO tagDTO) {
    log.debug("Creating new tag with name: {}", tagDTO.getName());

    // Перевірка існування тегу з такою назвою
    if (tagRepository.existsByName(tagDTO.getName())) {
      throw new ResourceAlreadyExistsException("Tag already exists with name: " + tagDTO.getName());
    }

    // Створення нового тегу
    Tag tag = Tag.builder()
            .name(tagDTO.getName())
            .build();

    Tag savedTag = tagRepository.save(tag);
    log.info("Tag created successfully with id: {}", savedTag.getId());

    return convertToDto(savedTag);
  }

  /**
   * Оновити тег
   * @param id ідентифікатор тегу
   * @param tagDTO нові дані тегу
   * @return DTO оновленого тегу
   * @throws ResourceNotFoundException якщо тег не знайдено
   * @throws ResourceAlreadyExistsException якщо тег з такою назвою вже існує
   */
  @Transactional
  public TagDTO updateTag(Long id, TagDTO tagDTO) {
    log.debug("Updating tag with id: {}", id);
    Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

    // Перевірка існування іншого тегу з такою назвою
    if (!tag.getName().equals(tagDTO.getName()) && tagRepository.existsByName(tagDTO.getName())) {
      throw new ResourceAlreadyExistsException("Another tag already exists with name: " + tagDTO.getName());
    }

    // Оновлення назви тегу
    tag.setName(tagDTO.getName());

    Tag updatedTag = tagRepository.save(tag);
    log.info("Tag updated successfully with id: {}", updatedTag.getId());

    return convertToDto(updatedTag);
  }

  /**
   * Видалити тег
   * @param id ідентифікатор тегу
   * @throws ResourceNotFoundException якщо тег не знайдено
   */
  @Transactional
  public void deleteTag(Long id) {
    log.debug("Deleting tag with id: {}", id);
    Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

    tagRepository.delete(tag);
    log.info("Tag deleted successfully with id: {}", id);
  }

  /**
   * Конвертувати сутність тегу в DTO
   * @param tag сутність тегу
   * @return DTO тегу
   */
  private TagDTO convertToDto(Tag tag) {
    TagDTO tagDTO = modelMapper.map(tag, TagDTO.class);

    // Додаємо кількість карток з цим тегом
    tagDTO.setCardCount(tag.getCards().size());

    return tagDTO;
  }
}
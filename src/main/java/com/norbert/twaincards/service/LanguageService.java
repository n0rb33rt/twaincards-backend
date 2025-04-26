package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.LanguageDTO;
import com.norbert.twaincards.entity.Language;
import com.norbert.twaincards.exception.ResourceAlreadyExistsException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з мовами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageService {

  private final LanguageRepository languageRepository;
  private final ModelMapper modelMapper;

  /**
   * Отримати всі мови
   * @return список DTO мов
   */
  @Transactional(readOnly = true)
  public List<LanguageDTO> getAllLanguages() {
    log.debug("Getting all languages");
    return languageRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати всі активні мови
   * @return список DTO активних мов
   */
  @Transactional(readOnly = true)
  public List<LanguageDTO> getAllEnabledLanguages() {
    log.debug("Getting all enabled languages");
    return languageRepository.findByIsEnabledTrue().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати мову за ідентифікатором
   * @param id ідентифікатор мови
   * @return DTO мови
   * @throws ResourceNotFoundException якщо мову не знайдено
   */
  @Transactional(readOnly = true)
  public LanguageDTO getLanguageById(Long id) {
    log.debug("Getting language by id: {}", id);
    Language language = languageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + id));
    return convertToDto(language);
  }

  /**
   * Отримати мову за кодом
   * @param code код мови
   * @return DTO мови
   * @throws ResourceNotFoundException якщо мову не знайдено
   */
  @Transactional(readOnly = true)
  public LanguageDTO getLanguageByCode(String code) {
    log.debug("Getting language by code: {}", code);
    Language language = languageRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with code: " + code));
    return convertToDto(language);
  }

  /**
   * Пошук мов за назвою або кодом
   * @param searchText текст для пошуку
   * @return список DTO знайдених мов
   */
  @Transactional(readOnly = true)
  public List<LanguageDTO> searchLanguages(String searchText) {
    log.debug("Searching languages with text: {}", searchText);
    return languageRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(searchText, searchText)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Створити нову мову
   * @param languageDTO дані мови
   * @return DTO створеної мови
   * @throws ResourceAlreadyExistsException якщо мова з таким кодом вже існує
   */
  @Transactional
  public LanguageDTO createLanguage(LanguageDTO languageDTO) {
    log.debug("Creating new language with code: {}", languageDTO.getCode());

    // Перевірка існування мови з таким кодом
    if (languageRepository.existsByCode(languageDTO.getCode())) {
      throw new ResourceAlreadyExistsException("Language already exists with code: " + languageDTO.getCode());
    }

    // Створення нової мови
    Language language = Language.builder()
            .code(languageDTO.getCode())
            .name(languageDTO.getName())
            .nativeName(languageDTO.getNativeName())
            .isEnabled(languageDTO.getIsEnabled() != null ? languageDTO.getIsEnabled() : true)
            .build();

    Language savedLanguage = languageRepository.save(language);
    log.info("Language created successfully: {}", savedLanguage.getCode());

    return convertToDto(savedLanguage);
  }

  /**
   * Оновити мову
   * @param id ідентифікатор мови
   * @param languageDTO нові дані мови
   * @return DTO оновленої мови
   * @throws ResourceNotFoundException якщо мову не знайдено
   */
  @Transactional
  public LanguageDTO updateLanguage(Long id, LanguageDTO languageDTO) {
    log.debug("Updating language with id: {}", id);
    Language language = languageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + id));

    // Оновлення даних мови
    language.setName(languageDTO.getName());
    language.setNativeName(languageDTO.getNativeName());

    if (languageDTO.getIsEnabled() != null) {
      language.setIsEnabled(languageDTO.getIsEnabled());
    }

    Language updatedLanguage = languageRepository.save(language);
    log.info("Language updated successfully: {}", updatedLanguage.getCode());

    return convertToDto(updatedLanguage);
  }

  /**
   * Активувати мову
   * @param id ідентифікатор мови
   * @throws ResourceNotFoundException якщо мову не знайдено
   */
  @Transactional
  public void enableLanguage(Long id) {
    log.debug("Enabling language with id: {}", id);
    Language language = languageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + id));

    language.setIsEnabled(true);
    languageRepository.save(language);
    log.info("Language enabled successfully: {}", language.getCode());
  }

  /**
   * Деактивувати мову
   * @param id ідентифікатор мови
   * @throws ResourceNotFoundException якщо мову не знайдено
   */
  @Transactional
  public void disableLanguage(Long id) {
    log.debug("Disabling language with id: {}", id);
    Language language = languageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + id));

    language.setIsEnabled(false);
    languageRepository.save(language);
    log.info("Language disabled successfully: {}", language.getCode());
  }

  /**
   * Конвертувати сутність мови в DTO
   * @param language сутність мови
   * @return DTO мови
   */
  private LanguageDTO convertToDto(Language language) {
    return modelMapper.map(language, LanguageDTO.class);
  }
}
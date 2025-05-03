package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.LanguageDTO;
import com.norbert.twaincards.entity.Language;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageService {

  private final LanguageRepository languageRepository;
  private final ModelMapper modelMapper;

  @Transactional(readOnly = true)
  public List<LanguageDTO> getAllLanguages() {
    return languageRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<LanguageDTO> getAllEnabledLanguages() {
    return languageRepository.findByIsEnabledTrue().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public LanguageDTO getLanguageById(Long id) {
    Language language = languageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + id));
    return convertToDto(language);
  }

  @Transactional(readOnly = true)
  public LanguageDTO getLanguageByCode(String code) {
    Language language = languageRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with code: " + code));
    return convertToDto(language);
  }

  @Transactional(readOnly = true)
  public List<LanguageDTO> searchLanguages(String searchText) {
    return languageRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(searchText, searchText)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

//  @Transactional
//  public LanguageDTO createLanguage(LanguageDTO languageDTO) {
//    if (languageRepository.existsByCode(languageDTO.getCode())) {
//      throw new ResourceAlreadyExistsException("Language already exists with code: " + languageDTO.getCode());
//    }
//
//    Language language = Language.builder()
//            .code(languageDTO.getCode())
//            .name(languageDTO.getName())
//            .nativeName(languageDTO.getNativeName())
//            .build();
//
//    Language savedLanguage = languageRepository.save(language);
//    log.info("Language created successfully: {}", savedLanguage.getCode());
//
//    return convertToDto(savedLanguage);
//  }

  @Transactional
  public LanguageDTO updateLanguage(Long id, LanguageDTO languageDTO) {
    Language language = languageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + id));

    language.setName(languageDTO.getName());
    language.setNativeName(languageDTO.getNativeName());

    Language updatedLanguage = languageRepository.save(language);
    log.info("Language updated successfully: {}", updatedLanguage.getCode());

    return convertToDto(updatedLanguage);
  }

  //todo: for admins delete add update

  private LanguageDTO convertToDto(Language language) {
    return modelMapper.map(language, LanguageDTO.class);
  }
}
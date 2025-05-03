package com.norbert.twaincards.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.entity.Card;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.Language;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.enumeration.ImportFormat;
import com.norbert.twaincards.exception.ImportException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.CardRepository;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.LanguageRepository;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

  private final CollectionRepository collectionRepository;
  private final CardRepository cardRepository;
  private final LanguageRepository languageRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @Transactional
  public CollectionDTO importCollection(MultipartFile file, ImportFormat format) {
    User currentUser = securityUtils.getCurrentUser();

    try {
      return switch (format) {
        case CSV -> importFromCsv(file, currentUser);
        case JSON -> importFromJson(file, currentUser);
        case XML -> importFromXml(file, currentUser);
      };
    } catch (IOException e) {
      log.error("Error importing collection in format: {}", format, e);
      throw new ImportException("Error importing data: " + e.getMessage());
    }
  }

  private CollectionDTO importFromCsv(MultipartFile file, User user) throws IOException {
    CsvSchema schema = CsvSchema.emptySchema().withHeader();
    CsvMapper csvMapper = new CsvMapper();

    // Fix the type compatibility issue
    List<Object> rawRows = csvMapper.readerFor(Map.class)
            .with(schema)
            .readValues(file.getInputStream())
            .readAll();

    // Convert to the required type with proper casting
    List<Map<String, String>> rows = new ArrayList<>();
    for (Object row : rawRows) {
      if (row instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, String> typedRow = (Map<String, String>) row;
        rows.add(typedRow);
      }
    }

    if (rows.isEmpty()) {
      throw new ImportException("CSV file is empty or has invalid format");
    }

    String collectionName = Objects.requireNonNull(file.getOriginalFilename()).replace(".csv", "");

    Language sourceLanguage = languageRepository.findByCode("en")
            .orElseThrow(() -> new ResourceNotFoundException("Default language not found"));
    Language targetLanguage = languageRepository.findByCode("en")
            .orElseThrow(() -> new ResourceNotFoundException("Default language not found"));

    Collection collection = Collection.builder()
            .user(user)
            .name(collectionName)
            .description("Imported from CSV")
            .sourceLanguage(sourceLanguage)
            .targetLanguage(targetLanguage)
            .isPublic(false)
            .build();

    Collection savedCollection = collectionRepository.save(collection);

    for (Map<String, String> row : rows) {
      if (row.containsKey("frontText") && row.containsKey("backText")) {
        Card card = Card.builder()
                .collection(savedCollection)
                .frontText(row.get("frontText"))
                .backText(row.get("backText"))
                .exampleUsage(row.getOrDefault("exampleUsage", ""))
                .build();

        cardRepository.save(card);
      }
    }

    return modelMapper.map(savedCollection, CollectionDTO.class);
  }

  private CollectionDTO importFromJson(MultipartFile file, User user) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map data = objectMapper.readValue(file.getInputStream(), Map.class);

    if (!data.containsKey("collection") || !data.containsKey("cards")) {
      throw new ImportException("JSON file has invalid format");
    }

    // Fixed type casting
    Map<String, Object> collectionData = (Map<String, Object>) data.get("collection");
    List<Object> rawCards = (List<Object>) data.get("cards");

    // Properly convert the list items
    List<Map<String, Object>> cardsData = new ArrayList<>();
    for (Object card : rawCards) {
      if (card instanceof Map) {
        cardsData.add((Map<String, Object>) card);
      }
    }

    String name = (String) collectionData.getOrDefault("name", Objects.requireNonNull(file.getOriginalFilename()).replace(".json", ""));
    String description = (String) collectionData.getOrDefault("description", "Imported from JSON");

    Language sourceLanguage;
    Language targetLanguage;

    if (collectionData.containsKey("sourceLanguageCode")) {
      sourceLanguage = languageRepository.findByCode((String) collectionData.get("sourceLanguageCode"))
              .orElseThrow(() -> new ResourceNotFoundException("Source language not found"));
    } else {
      sourceLanguage = languageRepository.findByCode("en")
              .orElseThrow(() -> new ResourceNotFoundException("Default language not found"));
    }

    if (collectionData.containsKey("targetLanguageCode")) {
      targetLanguage = languageRepository.findByCode((String) collectionData.get("targetLanguageCode"))
              .orElseThrow(() -> new ResourceNotFoundException("Target language not found"));
    } else {
      targetLanguage = languageRepository.findByCode("en")
              .orElseThrow(() -> new ResourceNotFoundException("Default language not found"));
    }

    Collection collection = Collection.builder()
            .user(user)
            .name(name)
            .description(description)
            .sourceLanguage(sourceLanguage)
            .targetLanguage(targetLanguage)
            .isPublic(false)
            .build();

    Collection savedCollection = collectionRepository.save(collection);

    for (Map<String, Object> cardData : cardsData) {
      if (cardData.containsKey("frontText") && cardData.containsKey("backText")) {
        Card card = Card.builder()
                .collection(savedCollection)
                .frontText((String) cardData.get("frontText"))
                .backText((String) cardData.get("backText"))
                .exampleUsage(cardData.containsKey("exampleUsage") ? (String) cardData.get("exampleUsage") : "")
                .build();

        cardRepository.save(card);
      }
    }

    return modelMapper.map(savedCollection, CollectionDTO.class);
  }

  private CollectionDTO importFromXml(MultipartFile file, User user) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    Map data = xmlMapper.readValue(file.getInputStream(), Map.class);

    if (!data.containsKey("collection") || !data.containsKey("cards")) {
      throw new ImportException("XML file has invalid format");
    }

    // Fixed type casting
    Map<String, Object> collectionData = (Map<String, Object>) data.get("collection");
    List<Object> rawCards = (List<Object>) data.get("cards");

    // Properly convert the list items
    List<Map<String, Object>> cardsData = new ArrayList<>();
    for (Object card : rawCards) {
      if (card instanceof Map) {
        cardsData.add((Map<String, Object>) card);
      }
    }

    String name = (String) collectionData.getOrDefault("name", file.getOriginalFilename().replace(".xml", ""));
    String description = (String) collectionData.getOrDefault("description", "Imported from XML");

    Language sourceLanguage;
    Language targetLanguage;

    if (collectionData.containsKey("sourceLanguageCode")) {
      sourceLanguage = languageRepository.findByCode((String) collectionData.get("sourceLanguageCode"))
              .orElseThrow(() -> new ResourceNotFoundException("Source language not found"));
    } else {
      sourceLanguage = languageRepository.findByCode("en")
              .orElseThrow(() -> new ResourceNotFoundException("Default language not found"));
    }

    if (collectionData.containsKey("targetLanguageCode")) {
      targetLanguage = languageRepository.findByCode((String) collectionData.get("targetLanguageCode"))
              .orElseThrow(() -> new ResourceNotFoundException("Target language not found"));
    } else {
      targetLanguage = languageRepository.findByCode("en")
              .orElseThrow(() -> new ResourceNotFoundException("Default language not found"));
    }

    Collection collection = Collection.builder()
            .user(user)
            .name(name)
            .description(description)
            .sourceLanguage(sourceLanguage)
            .targetLanguage(targetLanguage)
            .isPublic(false)
            .build();

    Collection savedCollection = collectionRepository.save(collection);

    for (Map<String, Object> cardData : cardsData) {
      if (cardData.containsKey("frontText") && cardData.containsKey("backText")) {
        Card card = Card.builder()
                .collection(savedCollection)
                .frontText((String) cardData.get("frontText"))
                .backText((String) cardData.get("backText"))
                .exampleUsage(cardData.containsKey("exampleUsage") ? (String) cardData.get("exampleUsage") : "")
                .build();

        cardRepository.save(card);
      }
    }

    return modelMapper.map(savedCollection, CollectionDTO.class);
  }
}
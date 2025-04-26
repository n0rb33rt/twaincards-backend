package com.norbert.twaincards.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.norbert.twaincards.dto.CardDTO;
import com.norbert.twaincards.dto.CollectionDTO;
import com.norbert.twaincards.entity.Card;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.ExportException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UnauthorizedAccessException;
import com.norbert.twaincards.repository.CardRepository;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервіс для експорту даних у різні формати
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

  private final UserRepository userRepository;
  private final CollectionRepository collectionRepository;
  private final CardRepository cardRepository;
  private final ModelMapper modelMapper;

  /**
   * Можливі формати експорту
   */
  public enum ExportFormat {
    CSV, JSON, XML
  }

  /**
   * Експортувати колекцію у вказаному форматі
   * @param userId ідентифікатор користувача
   * @param collectionId ідентифікатор колекції
   * @param format формат експорту
   * @return рядок даних у вказаному форматі
   * @throws ResourceNotFoundException якщо користувача або колекцію не знайдено
   * @throws UnauthorizedAccessException якщо користувач не має доступу до колекції
   * @throws ExportException якщо виникла помилка при експорті
   */
  @Transactional(readOnly = true)
  public String exportCollection(Long userId, Long collectionId, ExportFormat format) {
    log.debug("Exporting collection with id: {} in format: {}", collectionId, format);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Перевірка доступу до колекції
    if (!collection.getIsPublic() && !collection.getUser().getId().equals(userId)) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    // Отримуємо всі картки колекції
    List<Card> cards = cardRepository.findByCollection(collection);

    // Підготовка даних для експорту
    CollectionDTO collectionDTO = modelMapper.map(collection, CollectionDTO.class);

    // Додаткова інформація про мови
    if (collection.getSourceLanguage() != null) {
      collectionDTO.setSourceLanguageName(collection.getSourceLanguage().getName());
      collectionDTO.setSourceLanguageCode(collection.getSourceLanguage().getCode());
    }

    if (collection.getTargetLanguage() != null) {
      collectionDTO.setTargetLanguageName(collection.getTargetLanguage().getName());
      collectionDTO.setTargetLanguageCode(collection.getTargetLanguage().getCode());
    }

    List<CardDTO> cardDTOs = cards.stream()
            .map(card -> {
              CardDTO cardDTO = modelMapper.map(card, CardDTO.class);
              cardDTO.setCollectionId(collectionId);
              return cardDTO;
            })
            .collect(Collectors.toList());

    // Підготовка структури даних для експорту
    Map<String, Object> exportData = new HashMap<>();
    exportData.put("collection", collectionDTO);
    exportData.put("cards", cardDTOs);

    // Експорт у вказаному форматі
    try {
      switch (format) {
      case CSV:
        return exportToCsv(cardDTOs);
      case JSON:
        return exportToJson(exportData);
      case XML:
        return exportToXml(exportData);
      default:
        throw new ExportException("Unsupported export format: " + format);
      }
    } catch (IOException e) {
      log.error("Error exporting collection with id: {} in format: {}", collectionId, format, e);
      throw new ExportException("Error exporting data: " + e.getMessage());
    }
  }

  /**
   * Експортувати картки у форматі CSV
   * @param cards список DTO карток
   * @return рядок даних у форматі CSV
   * @throws IOException якщо виникла помилка при експорті
   */
  private String exportToCsv(List<CardDTO> cards) throws IOException {
    // Створюємо схему CSV
    CsvSchema.Builder schemaBuilder = CsvSchema.builder();
    schemaBuilder.addColumn("frontText");
    schemaBuilder.addColumn("backText");
    schemaBuilder.addColumn("phoneticText");
    schemaBuilder.addColumn("exampleUsage");

    CsvSchema schema = schemaBuilder.build().withHeader();

    // Підготовка списку спрощених карток для експорту
    List<Map<String, String>> simplifiedCards = new ArrayList<>();
    for (CardDTO card : cards) {
      Map<String, String> simplifiedCard = new HashMap<>();
      simplifiedCard.put("frontText", card.getFrontText());
      simplifiedCard.put("backText", card.getBackText());
      simplifiedCard.put("phoneticText", card.getPhoneticText() != null ? card.getPhoneticText() : "");
      simplifiedCard.put("exampleUsage", card.getExampleUsage() != null ? card.getExampleUsage() : "");
      simplifiedCards.add(simplifiedCard);
    }

    // Експорт у форматі CSV
    CsvMapper csvMapper = new CsvMapper();
    return csvMapper.writer(schema).writeValueAsString(simplifiedCards);
  }

  /**
   * Експортувати дані у форматі JSON
   * @param data дані для експорту
   * @return рядок даних у форматі JSON
   * @throws IOException якщо виникла помилка при експорті
   */
  private String exportToJson(Map<String, Object> data) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return objectMapper.writeValueAsString(data);
  }

  /**
   * Експортувати дані у форматі XML
   * @param data дані для експорту
   * @return рядок даних у форматі XML
   * @throws IOException якщо виникла помилка при експорті
   */
  private String exportToXml(Map<String, Object> data) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return xmlMapper.writeValueAsString(data);
  }
}
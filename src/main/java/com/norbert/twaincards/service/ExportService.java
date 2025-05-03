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
import com.norbert.twaincards.entity.enumeration.ExportFormat;
import com.norbert.twaincards.exception.ExportException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UnauthorizedAccessException;
import com.norbert.twaincards.repository.CardRepository;
import com.norbert.twaincards.repository.CollectionRepository;
import com.norbert.twaincards.util.SecurityUtils;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

  private final CollectionRepository collectionRepository;
  private final CardRepository cardRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @Transactional(readOnly = true)
  public String exportCollection(Long collectionId, ExportFormat format) {
    User currentUser = securityUtils.getCurrentUser();

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    if (!collection.getIsPublic() && !collection.getUser().getId().equals(currentUser.getId())) {
      throw new UnauthorizedAccessException("User does not have access to this collection");
    }

    List<Card> cards = cardRepository.findByCollection(collection);
    CollectionDTO collectionDTO = modelMapper.map(collection, CollectionDTO.class);

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

    Map<String, Object> exportData = new HashMap<>();
    exportData.put("collection", collectionDTO);
    exportData.put("cards", cardDTOs);

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
      log.error("Error exporting collection with id: {}", collectionId, e);
      throw new ExportException("Error exporting data: " + e.getMessage());
    }
  }

  private String exportToCsv(List<CardDTO> cards) throws IOException {
    CsvSchema.Builder schemaBuilder = CsvSchema.builder();
    schemaBuilder.addColumn("frontText");
    schemaBuilder.addColumn("backText");
    schemaBuilder.addColumn("exampleUsage");

    CsvSchema schema = schemaBuilder.build().withHeader();

    List<Map<String, String>> simplifiedCards = new ArrayList<>();
    for (CardDTO card : cards) {
      Map<String, String> simplifiedCard = new HashMap<>();
      simplifiedCard.put("frontText", card.getFrontText());
      simplifiedCard.put("backText", card.getBackText());
      simplifiedCard.put("exampleUsage", card.getExampleUsage() != null ? card.getExampleUsage() : "");
      simplifiedCards.add(simplifiedCard);
    }

    CsvMapper csvMapper = new CsvMapper();
    return csvMapper.writer(schema).writeValueAsString(simplifiedCards);
  }

  private String exportToJson(Map<String, Object> data) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return objectMapper.writeValueAsString(data);
  }

  private String exportToXml(Map<String, Object> data) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return xmlMapper.writeValueAsString(data);
  }
}
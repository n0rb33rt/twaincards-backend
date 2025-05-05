package com.norbert.twaincards.service;

import com.norbert.twaincards.entity.CommonWord;
import com.norbert.twaincards.repository.CommonWordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class CommonWordService {

    private final CommonWordRepository commonWordRepository;


    public Optional<String> findTranslation(String sourceText, String sourceLanguageCode, String targetLanguageCode) {
        String normalizedText = sourceText.toLowerCase().trim();
        Optional<CommonWord> commonWord = commonWordRepository.findByAnyTextAndLanguageCodes(
                normalizedText, sourceLanguageCode, targetLanguageCode);

        return commonWord.map(word -> {

            if (word.getSourceText().equalsIgnoreCase(normalizedText) &&
                    word.getSourceLanguageCode().equals(sourceLanguageCode) &&
                    word.getTargetLanguageCode().equals(targetLanguageCode)) {
                return word.getTargetText();
            } else {
                return word.getSourceText();
            }
        });
    }

} 
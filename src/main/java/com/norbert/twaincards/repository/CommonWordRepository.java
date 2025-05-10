package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.CommonWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommonWordRepository extends JpaRepository<CommonWord, Long> {

    @Query("SELECT cw FROM CommonWord cw WHERE " +
            "((cw.sourceText = :text AND cw.sourceLanguageCode = :langCode1 AND cw.targetLanguageCode = :langCode2) OR " +
            "(cw.targetText = :text AND cw.sourceLanguageCode = :langCode2 AND cw.targetLanguageCode = :langCode1))")
    Optional<CommonWord> findByAnyTextAndLanguageCodes(
            @Param("text") String text,
            @Param("langCode1") String languageCode1,
            @Param("langCode2") String languageCode2);
}

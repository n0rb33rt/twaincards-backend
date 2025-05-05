package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

  Optional<Language> findByCode(String code);


  boolean existsByCode(String code);

  List<Language> findByNameContainingIgnoreCase(String name);
}
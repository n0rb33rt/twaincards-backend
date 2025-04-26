package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторій для роботи з мовами
 */
@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

  /**
   * Пошук мови за кодом
   * @param code код мови
   * @return опціональний об'єкт мови
   */
  Optional<Language> findByCode(String code);

  /**
   * Перевірка існування мови з вказаним кодом
   * @param code код мови
   * @return true, якщо мова існує
   */
  boolean existsByCode(String code);

  /**
   * Отримання списку всіх активних мов
   * @return список активних мов
   */
  List<Language> findByIsEnabledTrue();

  /**
   * Пошук мов за назвою або кодом, що містять вказаний текст
   * @param text текст для пошуку
   * @return список знайдених мов
   */
  List<Language> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
}
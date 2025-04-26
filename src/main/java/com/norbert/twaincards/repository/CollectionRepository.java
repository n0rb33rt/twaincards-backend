package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.Language;
import com.norbert.twaincards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторій для роботи з колекціями карток
 */
@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

  /**
   * Отримання списку колекцій користувача
   * @param user користувач
   * @return список колекцій
   */
  List<Collection> findByUser(User user);

  /**
   * Отримання сторінки колекцій користувача
   * @param user користувач
   * @param pageable параметри пагінації
   * @return сторінка колекцій
   */
  Page<Collection> findByUser(User user, Pageable pageable);

  /**
   * Отримання списку публічних колекцій
   * @return список публічних колекцій
   */
  List<Collection> findByIsPublicTrue();

  /**
   * Отримання сторінки публічних колекцій
   * @param pageable параметри пагінації
   * @return сторінка публічних колекцій
   */
  Page<Collection> findByIsPublicTrue(Pageable pageable);

  /**
   * Пошук колекції за користувачем та назвою колекції
   * @param user користувач
   * @param name назва колекції
   * @return опціональний об'єкт колекції
   */
  Optional<Collection> findByUserAndName(User user, String name);

  /**
   * Отримання колекції з усіма зв'язаними даними
   * @param id ідентифікатор колекції
   * @return опціональний об'єкт колекції
   */
  @Query("SELECT c FROM Collection c LEFT JOIN FETCH c.sourceLanguage LEFT JOIN FETCH c.targetLanguage WHERE c.id = :id")
  Optional<Collection> findByIdWithLanguages(@Param("id") Long id);

  /**
   * Пошук колекцій за мовами
   * @param sourceLanguage мова оригіналу
   * @param targetLanguage мова перекладу
   * @return список колекцій
   */
  List<Collection> findBySourceLanguageAndTargetLanguageAndIsPublicTrue(Language sourceLanguage, Language targetLanguage);

  /**
   * Пошук колекцій за мовою оригіналу
   * @param sourceLanguage мова оригіналу
   * @return список колекцій
   */
  List<Collection> findBySourceLanguageAndIsPublicTrue(Language sourceLanguage);

  /**
   * Пошук колекцій за мовою перекладу
   * @param targetLanguage мова перекладу
   * @return список колекцій
   */
  List<Collection> findByTargetLanguageAndIsPublicTrue(Language targetLanguage);

  /**
   * Пошук колекцій за текстом в назві або описі
   * @param searchText текст для пошуку
   * @param pageable параметри пагінації
   * @return сторінка знайдених колекцій
   */
  @Query("SELECT c FROM Collection c WHERE c.isPublic = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
  Page<Collection> searchPublicCollections(@Param("searchText") String searchText, Pageable pageable);

  /**
   * Пошук колекцій користувача за текстом в назві або описі
   * @param user користувач
   * @param searchText текст для пошуку
   * @param pageable параметри пагінації
   * @return сторінка знайдених колекцій
   */
  @Query("SELECT c FROM Collection c WHERE c.user = :user AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
  Page<Collection> searchUserCollections(@Param("user") User user, @Param("searchText") String searchText, Pageable pageable);
}
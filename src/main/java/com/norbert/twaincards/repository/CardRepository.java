package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Card;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторій для роботи з картками
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

  /**
   * Отримання списку карток колекції
   * @param collection колекція
   * @return список карток
   */
  List<Card> findByCollection(Collection collection);

  /**
   * Отримання сторінки карток колекції
   * @param collection колекція
   * @param pageable параметри пагінації
   * @return сторінка карток
   */
  Page<Card> findByCollection(Collection collection, Pageable pageable);

  /**
   * Отримання кількості карток в колекції
   * @param collectionId ідентифікатор колекції
   * @return кількість карток
   */
  @Query("SELECT COUNT(c) FROM Card c WHERE c.collection.id = :collectionId")
  Long countByCollectionId(@Param("collectionId") Long collectionId);

  /**
   * Пошук карток за текстом на лицьовій стороні
   * @param frontText текст для пошуку
   * @param collection колекція
   * @return список карток
   */
  List<Card> findByFrontTextContainingIgnoreCaseAndCollection(String frontText, Collection collection);

  /**
   * Пошук карток за текстом на зворотній стороні
   * @param backText текст для пошуку
   * @param collection колекція
   * @return список карток
   */
  List<Card> findByBackTextContainingIgnoreCaseAndCollection(String backText, Collection collection);

  /**
   * Пошук карток за текстом на лицьовій або зворотній стороні
   * @param text текст для пошуку
   * @param collection колекція
   * @return список карток
   */
  @Query("SELECT c FROM Card c WHERE c.collection = :collection AND (LOWER(c.frontText) LIKE LOWER(CONCAT('%', :text, '%')) OR LOWER(c.backText) LIKE LOWER(CONCAT('%', :text, '%')))")
  List<Card> findByTextInCard(@Param("text") String text, @Param("collection") Collection collection);

  /**
   * Пошук карток за тегом
   * @param tag тег
   * @return список карток
   */
  List<Card> findByTagsContaining(Tag tag);

  /**
   * Пошук карток за тегом та колекцією
   * @param tag тег
   * @param collection колекція
   * @return список карток
   */
  @Query("SELECT c FROM Card c JOIN c.tags t WHERE t = :tag AND c.collection = :collection")
  List<Card> findByTagAndCollection(@Param("tag") Tag tag, @Param("collection") Collection collection);

  /**
   * Отримання карток з детальною інформацією
   * @param collectionId ідентифікатор колекції
   * @param pageable параметри пагінації
   * @return сторінка карток
   */
  @Query("SELECT DISTINCT c FROM Card c LEFT JOIN FETCH c.tags WHERE c.collection.id = :collectionId")
  Page<Card> findByCollectionIdWithTags(@Param("collectionId") Long collectionId, Pageable pageable);
}
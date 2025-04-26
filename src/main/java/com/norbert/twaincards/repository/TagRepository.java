package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторій для роботи з тегами
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

  /**
   * Пошук тегу за назвою
   * @param name назва тегу
   * @return опціональний об'єкт тегу
   */
  Optional<Tag> findByName(String name);

  /**
   * Перевірка існування тегу з вказаним ім'ям
   * @param name назва тегу
   * @return true, якщо тег існує
   */
  boolean existsByName(String name);

  /**
   * Пошук тегів за частковою назвою
   * @param name частина назви тегу
   * @return список тегів
   */
  List<Tag> findByNameContainingIgnoreCase(String name);

  /**
   * Отримання тегів колекції
   * @param collectionId ідентифікатор колекції
   * @return список тегів
   */
  @Query("SELECT DISTINCT t FROM Tag t JOIN t.cards c WHERE c.collection.id = :collectionId")
  List<Tag> findTagsByCollectionId(@Param("collectionId") Long collectionId);

  /**
   * Отримання найпопулярніших тегів
   * @param limit кількість тегів
   * @return список тегів з кількістю карток
   */
  @Query("SELECT t, COUNT(c) as cardCount FROM Tag t JOIN t.cards c GROUP BY t ORDER BY cardCount DESC")
  List<Object[]> findMostPopularTags(Pageable pageable);
}
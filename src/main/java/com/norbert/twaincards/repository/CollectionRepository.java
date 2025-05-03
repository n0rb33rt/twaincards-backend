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


@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

  Boolean existsCollectionByUserAndId(User user, Long id);

  List<Collection> findByUser(User user);

  Page<Collection> findByUser(User user, Pageable pageable);

  List<Collection> findByIsPublicTrue();

  Page<Collection> findByIsPublicTrue(Pageable pageable);

  Optional<Collection> findByUserAndName(User user, String name);

  @Query("SELECT c FROM Collection c LEFT JOIN FETCH c.sourceLanguage LEFT JOIN FETCH c.targetLanguage WHERE c.id = :id")
  Optional<Collection> findByIdWithLanguages(@Param("id") Long id);

  List<Collection> findBySourceLanguageAndTargetLanguageAndIsPublicTrue(Language sourceLanguage, Language targetLanguage);

  List<Collection> findBySourceLanguageAndIsPublicTrue(Language sourceLanguage);

  List<Collection> findByTargetLanguageAndIsPublicTrue(Language targetLanguage);

  @Query("SELECT c FROM Collection c WHERE c.isPublic = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
  Page<Collection> searchPublicCollections(@Param("searchText") String searchText, Pageable pageable);

  @Query("SELECT c FROM Collection c WHERE c.user = :user AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
  Page<Collection> searchUserCollections(@Param("user") User user, @Param("searchText") String searchText, Pageable pageable);
}
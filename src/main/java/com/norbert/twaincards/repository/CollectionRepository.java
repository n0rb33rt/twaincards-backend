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


  List<Collection> findByUser(User user);

  Page<Collection> findByUser(User user, Pageable pageable);


  Page<Collection> findByIsPublicTrue(Pageable pageable);

  Optional<Collection> findByUserAndName(User user, String name);

  @Query("SELECT c FROM Collection c LEFT JOIN FETCH c.sourceLanguage LEFT JOIN FETCH c.targetLanguage WHERE c.id = :id")
  Optional<Collection> findByIdWithLanguages(@Param("id") Long id);

  @Query("SELECT c FROM Collection c WHERE c.isPublic = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
  Page<Collection> searchPublicCollections(@Param("searchText") String searchText, Pageable pageable);

  @Query("SELECT c FROM Collection c WHERE c.user = :user AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
  Page<Collection> searchUserCollections(@Param("user") User user, @Param("searchText") String searchText, Pageable pageable);

  @Query("SELECT DISTINCT c FROM Collection c " +
         "LEFT JOIN StudySession s ON c IN (SELECT sc FROM s.studiedCollections sc) " +
         "WHERE c.user = :user OR (s.user = :user AND c.isPublic = true) " +
         "ORDER BY c.updatedAt DESC")
  List<Collection> findRecentlyInteractedCollections(@Param("user") User user, Pageable pageable);
}
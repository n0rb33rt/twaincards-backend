package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Tag;
import com.norbert.twaincards.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

  List<Tag> findByUser(User user);

  Optional<Tag> findByIdAndUser(Long id, User user);

  Optional<Tag> findByNameAndUser(String name, User user);

  List<Tag> findByNameContainingIgnoreCaseAndUser(String name, User user);

  boolean existsByNameAndUser(String name, User user);

  Optional<Tag> findByName(String name);
//
//  @Query("SELECT t FROM Tag t JOIN t.cards c JOIN c.collections col WHERE col.id = :collectionId AND t.user = :user")
//  List<Tag> findTagsByCollectionIdAndUser(@Param("collectionId") Long collectionId, @Param("user") User user);
//
//  @Query("SELECT t FROM Tag t JOIN t.cards c WHERE c.collection.id = :collectionId")
//  List<Tag> findTagsByCollectionId(@Param("collectionId") Long collectionId);
//
  @Query("SELECT t, COUNT(c) as cardCount FROM Tag t JOIN t.cards c WHERE t.user = :user GROUP BY t ORDER BY cardCount DESC")
  List<Object[]> findMostPopularTagsByUser(@Param("user") User user, Pageable pageable);

  @Query("SELECT t, COUNT(c) as cardCount FROM Tag t JOIN t.cards c GROUP BY t ORDER BY cardCount DESC")
  List<Object[]> findMostPopularTags(Pageable pageable);
}
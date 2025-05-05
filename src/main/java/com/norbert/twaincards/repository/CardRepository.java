package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Card;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CardRepository extends JpaRepository<Card, Long> {


  List<Card> findByCollection(Collection collection);


  Page<Card> findByCollection(Collection collection, Pageable pageable);

  @Query("SELECT COUNT(c) FROM Card c WHERE c.collection.id = :collectionId")
  Long countByCollectionId(@Param("collectionId") Long collectionId);
  
  @Query("SELECT COUNT(c) FROM Card c WHERE c.collection = :collection AND (c.collection.isPublic = true OR c.collection.user = :user)")
  Long countByCollectionAndUserAccess(@Param("collection") Collection collection, @Param("user") User user);

  @Query("SELECT c FROM Card c WHERE c.collection = :collection AND (LOWER(c.frontText) LIKE LOWER(CONCAT('%', :text, '%')) OR LOWER(c.backText) LIKE LOWER(CONCAT('%', :text, '%')))")
  List<Card> findByTextInCard(@Param("text") String text, @Param("collection") Collection collection);

}
package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.CollectionUserUsage;
import com.norbert.twaincards.entity.CollectionUserUsage.CollectionUserUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionUserUsageRepository extends JpaRepository<CollectionUserUsage, CollectionUserUsageId> {

    /**
     * Find a record by collection id and user id
     * 
     * @param collectionId the collection ID
     * @param userId the user ID
     * @return the usage record if exists
     */
    Optional<CollectionUserUsage> findByIdCollectionIdAndIdUserId(Long collectionId, Long userId);
    
    /**
     * Find all usage records for a specific collection
     * 
     * @param collectionId the collection ID
     * @return list of usage records
     */
    List<CollectionUserUsage> findByIdCollectionId(Long collectionId);
    
    /**
     * Find all usage records for a specific user
     * 
     * @param userId the user ID
     * @return list of usage records
     */
    List<CollectionUserUsage> findByIdUserId(Long userId);
    
    /**
     * Count the number of distinct users who have used a collection
     * 
     * @param collectionId the collection ID
     * @return the count of unique users
     */
    @Query("SELECT COUNT(DISTINCT cu.id.userId) FROM CollectionUserUsage cu WHERE cu.id.collectionId = :collectionId")
    Integer countUniqueUsersByCollectionId(@Param("collectionId") Long collectionId);
} 
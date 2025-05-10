package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.StudySession;
import com.norbert.twaincards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUser(User user);

    Page<StudySession> findByUser(User user, Pageable pageable);

    @Query("SELECT s FROM StudySession s WHERE s.user = :user AND s.startTime >= :startDate ORDER BY s.startTime DESC")
    List<StudySession> findRecentSessions(@Param("user") User user, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT s FROM StudySession s WHERE s.user = :user AND s.isCompleted = true ORDER BY s.startTime DESC")
    List<StudySession> findCompletedSessions(@Param("user") User user, Pageable pageable);

    @Query("SELECT s FROM StudySession s JOIN s.studiedCollections c WHERE c.id = :collectionId AND s.user = :user ORDER BY s.startTime DESC")
    List<StudySession> findByUserAndCollection(@Param("user") User user, @Param("collectionId") Long collectionId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM StudySession s WHERE s.user = :user AND s.isCompleted = true AND s.startTime >= :startDate")
    Long countSessionsInPeriod(@Param("user") User user, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(s.correctAnswers) FROM StudySession s WHERE s.user = :user AND s.isCompleted = true AND s.startTime >= :startDate")
    Long countCorrectAnswersInPeriod(@Param("user") User user, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(s.cardsReviewed) FROM StudySession s WHERE s.user = :user AND s.isCompleted = true AND s.startTime >= :startDate")
    Long countCardsReviewedInPeriod(@Param("user") User user, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM StudySession s JOIN s.studiedCollections c WHERE c.id = :collectionId")
    Long countUniqueUsersByCollection(@Param("collectionId") Long collectionId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM session_collections WHERE collection_id = :collectionId", nativeQuery = true)
    void removeCollectionFromAllStudySessions(@Param("collectionId") Long collectionId);
} 
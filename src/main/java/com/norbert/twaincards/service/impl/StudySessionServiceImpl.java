package com.norbert.twaincards.service.impl;

import com.norbert.twaincards.dto.StudySessionDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.StudySession;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.UserStatistics;
import com.norbert.twaincards.entity.enumeration.ActionType;
import com.norbert.twaincards.entity.enumeration.LearningStatus;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.*;
import com.norbert.twaincards.service.StudySessionService;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudySessionServiceImpl implements StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final CollectionRepository collectionRepository;
    private final SecurityUtils securityUtils;
    private final UserStatisticsRepository userStatisticsRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    
    @Override
    @Transactional
    public StudySessionDTO createSession(StudySessionDTO.CreateSessionRequest createRequest) {
        User currentUser = securityUtils.getCurrentUser();
        Collection collection = collectionRepository.findById(createRequest.getCollectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + createRequest.getCollectionId()));
        StudySession studySession = StudySession.builder()
                .user(currentUser)
                .deviceType(createRequest.getDeviceType())
                .platform(createRequest.getPlatform())
                .cardsReviewed(0)
                .correctAnswers(0)
                .isCompleted(false)
                .studiedCollections(Set.of(collection))
                .startTime(LocalDateTime.now())
                .build();
                
        StudySession savedSession = studySessionRepository.save(studySession);
        log.info("Created new study session with id: {} for user: {}", savedSession.getId(), currentUser.getUsername());
        
        return convertToDTO(savedSession);
    }
    
    @Override
    @Transactional(readOnly = true)
    public StudySessionDTO getSessionById(Long sessionId) {
        StudySession session = getSessionEntityById(sessionId);
        return convertToDTO(session);
    }

    @Override
    @Transactional
    public StudySessionDTO.SessionSummary completeSession(StudySessionDTO.CompleteSessionRequest completeRequest) {
        StudySession session = getSessionEntityById(completeRequest.getSessionId());
        User user = session.getUser();

        // Calculate statistics from learning history
        long historyCardsReviewed = learningHistoryRepository.countByStudySessionIdAndActionType(
                session.getId(), ActionType.REVIEW);

        long historyCorrectAnswers = learningHistoryRepository.countByStudySessionIdAndActionTypeAndIsCorrect(
                session.getId(), ActionType.REVIEW, true);

        // Use values from request if provided, otherwise use calculated values
        int cardsReviewed = completeRequest.getCardsReviewed() != null 
                ? completeRequest.getCardsReviewed() 
                : (int) historyCardsReviewed;
                
        int correctAnswers = completeRequest.getCorrectAnswers() != null 
                ? completeRequest.getCorrectAnswers() 
                : (int) historyCorrectAnswers;

        // Set values and complete the session
        session.setCardsReviewed(cardsReviewed);
        session.setCorrectAnswers(correctAnswers);
        session.completeSession();

        StudySession savedSession = studySessionRepository.save(session);
        log.info("Completed study session with id: {}", session.getId());

        // Update user statistics
        updateUserStatistics(user);

        // Check if collection exists before accessing
        if (session.getStudiedCollections().isEmpty()) {
            throw new IllegalStateException("No collections associated with this study session");
        }
        Collection collection = session.getStudiedCollections().iterator().next();

        return StudySessionDTO.SessionSummary.builder()
                .sessionId(savedSession.getId())
                .cardsStudied(savedSession.getCardsReviewed())
                .correctAnswers(savedSession.getCorrectAnswers())
                .incorrectAnswers(savedSession.getCardsReviewed() - savedSession.getCorrectAnswers())
                .successRate(savedSession.getAccuracyRate())
                .collectionName(collection.getName())
                .collectionId(collection.getId())
                .timeSpentSeconds((long) savedSession.getSessionDurationSeconds())
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public Page<StudySessionDTO.SessionSummary> getUserSessionSummaries(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<StudySession> sessionsPage = studySessionRepository.findByUser(currentUser, pageable);
        
        return sessionsPage.map(this::convertToSummary);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<StudySessionDTO.SessionSummary> getRecentSessionsForCollection(Long collectionId, int limit) {
        User currentUser = securityUtils.getCurrentUser();
        Pageable pageable = Pageable.ofSize(limit);

        List<StudySession> sessions = studySessionRepository.findByUserAndCollection(
                currentUser, collectionId, pageable);

        return sessions.stream()
                .map(this::convertToSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudySessionDTO.StudySessionStats getUserStudyStats(LocalDateTime startDate) {
        User currentUser = securityUtils.getCurrentUser();

        // Get stats from repository
        Long totalSessions = studySessionRepository.countSessionsInPeriod(currentUser, startDate);
        Long totalCardsReviewed = studySessionRepository.countCardsReviewedInPeriod(currentUser, startDate);
        Long totalCorrectAnswers = studySessionRepository.countCorrectAnswersInPeriod(currentUser, startDate);

        // Handle null values from database
        totalSessions = totalSessions != null ? totalSessions : 0L;
        totalCardsReviewed = totalCardsReviewed != null ? totalCardsReviewed : 0L;
        totalCorrectAnswers = totalCorrectAnswers != null ? totalCorrectAnswers : 0L;

        // Calculate derived stats
        Double averageAccuracy = totalCardsReviewed > 0
                ? (totalCorrectAnswers * 100.0) / totalCardsReviewed
                : 0.0;

        // Get session time data
        List<StudySession> completedSessions = studySessionRepository.findRecentSessions(currentUser, startDate)
                .stream()
                .filter(StudySession::getIsCompleted)
                .collect(Collectors.toList());


        return StudySessionDTO.StudySessionStats.builder()
                .totalSessions(totalSessions)
                .totalCardsReviewed(totalCardsReviewed)
                .totalCorrectAnswers(totalCorrectAnswers)
                .averageAccuracy(averageAccuracy)
                .build();
    }

    // Helper methods
    
    private StudySession getSessionEntityById(Long sessionId) {
        User currentUser = securityUtils.getCurrentUser();
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found with id: " + sessionId));

        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Study session not found with id: " + sessionId);
        }
        
        return session;
    }
    
    private StudySessionDTO convertToDTO(StudySession session) {
        List<Long> collectionIds = session.getStudiedCollections().stream()
                .map(Collection::getId)
                .collect(Collectors.toList());
        
        return StudySessionDTO.builder()
                .id(session.getId())
                .cardsReviewed(session.getCardsReviewed())
                .correctAnswers(session.getCorrectAnswers())
                .accuracyRate(session.getAccuracyRate())
                .isCompleted(session.getIsCompleted())
                .collectionIds(collectionIds)
                .build();
    }
    
    private StudySessionDTO.SessionSummary convertToSummary(StudySession session) {
        Collection collection = session.getStudiedCollections().isEmpty() 
                ? null 
                : session.getStudiedCollections().iterator().next();
        
        Integer incorrectAnswers = session.getCardsReviewed() - session.getCorrectAnswers();
        
        return StudySessionDTO.SessionSummary.builder()
                .sessionId(session.getId())
                .cardsStudied(session.getCardsReviewed())
                .correctAnswers(session.getCorrectAnswers())
                .incorrectAnswers(incorrectAnswers)
                .successRate(session.getAccuracyRate())
                .collectionName(collection != null ? collection.getName() : null)
                .collectionId(collection != null ? collection.getId() : null)
                .timeSpentSeconds(session.getSessionDurationSeconds() != null ? 
                        session.getSessionDurationSeconds().longValue() : null)
                .build();
    }
    
    /**
     * Updates the user statistics after completing a study session
     * @param user The user whose statistics need to be updated
     */
    private void updateUserStatistics(User user) {
        try {
            // Get or create user statistics
            UserStatistics statistics = userStatisticsRepository.findByUser(user)
                    .orElse(UserStatistics.builder().user(user).build());
            
            // Update learning streak
            LocalDate today = LocalDate.now();
            if (statistics.getLastStudyDate() == null || !statistics.getLastStudyDate().equals(today)) {
                if (statistics.getLastStudyDate() != null && 
                    statistics.getLastStudyDate().plusDays(1).equals(today)) {
                    // Increment streak only if last study was yesterday
                    statistics.setLearningStreakDays(statistics.getLearningStreakDays() + 1);
                } else if (statistics.getLastStudyDate() == null || 
                          !statistics.getLastStudyDate().equals(today)) {
                    // Reset streak if more than 1 day gap or first time
                    statistics.setLearningStreakDays(1);
                }
                statistics.setLastStudyDate(today);
            }
            
            // Update card statistics
            Long learnedCards = learningProgressRepository.countKnownCardsByUser(user);
            statistics.setLearnedCards(learnedCards.intValue());
            
            // Count in-progress cards (learning + review)
            Long learningCards = learningProgressRepository.countLearningCardsByUser(user);
            Long reviewCards = learningProgressRepository.countReviewCardsByUser(user);
            Long inProgressCards = learningCards + reviewCards;
            statistics.setCardsInProgress(inProgressCards.intValue());
            
            // Calculate total cards (learned + in progress + to learn)
            Long totalCards = learnedCards + inProgressCards;
            statistics.setTotalCards(totalCards.intValue());
            
            // Calculate to learn as derived value
            int toLearnCards = statistics.getTotalCards() - statistics.getLearnedCards() - statistics.getCardsInProgress();
            statistics.setCardsToLearn(Math.max(0, toLearnCards));
            
            userStatisticsRepository.save(statistics);
            log.info("Updated user statistics for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Error updating user statistics after session completion", e);
            // Continue execution despite the error
        }
    }
} 
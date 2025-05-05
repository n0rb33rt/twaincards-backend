package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.StudySessionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StudySessionService {
    
    /**
     * Create a new study session
     *
     * @param createRequest the session creation request
     * @return the created study session DTO
     */
    StudySessionDTO createSession(StudySessionDTO.CreateSessionRequest createRequest);
    
    /**
     * Get a session by ID
     *
     * @param sessionId the session ID
     * @return the session DTO
     */
    StudySessionDTO getSessionById(Long sessionId);
    
    /**
     * Complete a study session and generate summary
     *
     * @param completeRequest the session completion request
     * @return the session summary
     */
    StudySessionDTO.SessionSummary completeSession(StudySessionDTO.CompleteSessionRequest completeRequest);
    
    /**
     * Get session summaries for current user
     *
     * @param pageable pagination information
     * @return page of session summaries
     */
    Page<StudySessionDTO.SessionSummary> getUserSessionSummaries(Pageable pageable);
    
    /**
     * Get recent session summaries for a specific collection
     *
     * @param collectionId the collection ID
     * @param limit maximum number of results to return
     * @return list of session summaries
     */
    List<StudySessionDTO.SessionSummary> getRecentSessionsForCollection(Long collectionId, int limit);
    
    /**
     * Get study stats for the current user
     *
     * @param startDate the start date for statistics calculation
     * @return study session stats
     */
    StudySessionDTO.StudySessionStats getUserStudyStats(LocalDateTime startDate);
} 
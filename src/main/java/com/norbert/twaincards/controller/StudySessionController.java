package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.ApiError;
import com.norbert.twaincards.dto.StudyLimitDTO;
import com.norbert.twaincards.dto.StudySessionDTO;
import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.entity.Role;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.service.StudyLimitService;
import com.norbert.twaincards.service.StudySessionService;
import com.norbert.twaincards.service.UserService;
import com.norbert.twaincards.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/study-sessions")
@RequiredArgsConstructor
@Slf4j
public class StudySessionController {

    private final StudySessionService studySessionService;
    private final StudyLimitService studyLimitService;
    private final SecurityUtils securityUtils;


    @PostMapping
    public ResponseEntity<?> createSession(@Valid @RequestBody StudySessionDTO.CreateSessionRequest createRequest) {
        log.info("Request to create a new study session for collection: {}", createRequest.getCollectionId());

        if (!studyLimitService.canStudyMoreCards()) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiError("Daily card limit reached", 
                      "You have reached your daily limit of 30 cards. Upgrade to Premium for unlimited studying."));
        }

        StudySessionDTO sessionDTO = studySessionService.createSession(createRequest);
        return ResponseEntity.ok(sessionDTO);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<StudySessionDTO> getSession(@PathVariable Long sessionId) {
        log.info("Request to get study session with id: {}", sessionId);
        StudySessionDTO sessionDTO = studySessionService.getSessionById(sessionId);
        return ResponseEntity.ok(sessionDTO);
    }

    @PostMapping("/complete")
    public ResponseEntity<StudySessionDTO.SessionSummary> completeSession(
            @RequestBody @Valid StudySessionDTO.CompleteSessionRequest completeRequest) {
        log.info("Request to complete study session with id: {}", completeRequest.getSessionId());
        StudySessionDTO.SessionSummary summary = studySessionService.completeSession(completeRequest);
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/user/summaries")
    public ResponseEntity<Page<StudySessionDTO.SessionSummary>> getUserSessionSummaries(Pageable pageable) {
        log.info("Request to get study session summaries for current user");
        Page<StudySessionDTO.SessionSummary> summaries = studySessionService.getUserSessionSummaries(pageable);
        return ResponseEntity.ok(summaries);
    }
    
    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<List<StudySessionDTO.SessionSummary>> getSessionsForCollection(
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Request to get {} recent sessions for collection: {}", limit, collectionId);
        List<StudySessionDTO.SessionSummary> sessions = studySessionService.getRecentSessionsForCollection(collectionId, limit);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/stats")
    public ResponseEntity<StudySessionDTO.StudySessionStats> getStudyStats(
            @RequestParam(defaultValue = "30") int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        log.info("Request to get study stats for the last {} days", days);
        StudySessionDTO.StudySessionStats stats = studySessionService.getUserStudyStats(startDate);
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/limit-status")
    public ResponseEntity<StudyLimitDTO> getStudyLimitStatus() {
        User currentUser = securityUtils.getCurrentUser();
        boolean hasLimit = Role.ROLE_USER.equals(currentUser.getRole().getName());
        
        StudyLimitDTO limitDTO = new StudyLimitDTO();
        limitDTO.setHasLimit(hasLimit);
        limitDTO.setDailyLimit(hasLimit ? 30 : -1);
        limitDTO.setRemainingCards(studyLimitService.getRemainingCardLimit(currentUser));
        
        return ResponseEntity.ok(limitDTO);
    }
} 
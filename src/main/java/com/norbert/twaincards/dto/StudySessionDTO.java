package com.norbert.twaincards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudySessionDTO {
    private Long id;
    private Integer cardsReviewed;
    private Integer correctAnswers;
    private Double accuracyRate;
    private Boolean isCompleted;
    private List<Long> collectionIds;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSessionRequest {
        @NotNull
        private Long collectionId;
        private String deviceType;
        private String platform;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteSessionRequest {
        @NotNull
        private Long sessionId;
        private Integer cardsReviewed;
        private Integer correctAnswers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummary {
        private Long sessionId;
        private Integer cardsStudied;
        private Integer correctAnswers;
        private Integer incorrectAnswers;
        private Double successRate;
        private String collectionName;
        private Long collectionId;
        private Long timeSpentSeconds;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudySessionStats {
        private Long totalSessions;
        private Long totalCardsReviewed;
        private Long totalCorrectAnswers;
        private Double averageAccuracy;
    }
} 
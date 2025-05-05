package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for representing a user's study limit status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyLimitDTO {
    
    /**
     * Whether the user has a daily card limit
     */
    private boolean hasLimit;
    
    /**
     * The maximum number of cards this user can study per day
     * -1 indicates unlimited
     */
    private int dailyLimit;
    
    /**
     * The number of cards the user can still study today
     * -1 indicates unlimited
     */
    private int remainingCards;
} 
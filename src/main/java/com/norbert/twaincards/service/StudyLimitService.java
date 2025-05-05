package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.entity.Role;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.repository.LearningHistoryRepository;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service to enforce study limits based on user role
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyLimitService {

    private static final int DAILY_CARD_LIMIT_FOR_BASIC_USERS = 30;
    
    private final LearningHistoryRepository learningHistoryRepository;
    private final SecurityUtils securityUtils;

    public boolean canStudyMoreCards() {
        User user = securityUtils.getCurrentUser();
        if (Role.ROLE_PREMIUM.equals(user.getRole().getName()) || Role.ROLE_ADMIN.equals(user.getRole().getName())) {
            return true;
        }

        // For basic users, enforce the daily limit
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        Long cardsStudiedToday = learningHistoryRepository.countByUserIdAndPerformedAtBetween(
                user.getId(), startOfDay, endOfDay);
        
        log.debug("User {} has studied {} cards today. Limit is {}",
                user.getUsername(), cardsStudiedToday, DAILY_CARD_LIMIT_FOR_BASIC_USERS);
        
        return cardsStudiedToday < DAILY_CARD_LIMIT_FOR_BASIC_USERS;
    }
    

    public int getRemainingCardLimit(User user) {
        if (Role.ROLE_PREMIUM.equals(user.getRole().getName()) || Role.ROLE_ADMIN.equals(user.getRole().getName())) {
            return -1; // Unlimited
        }
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        Long cardsStudiedToday = learningHistoryRepository.countByUserIdAndPerformedAtBetween(
                user.getId(), startOfDay, endOfDay);
        
        int remaining = DAILY_CARD_LIMIT_FOR_BASIC_USERS - cardsStudiedToday.intValue();
        return Math.max(0, remaining);
    }
} 
package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for representing API errors in responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    
    private String status;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public ApiError(String status, String message) {
        this.status = status;
        this.message = message;
    }
} 
package com.norbert.twaincards.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPasswordResetRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;
} 
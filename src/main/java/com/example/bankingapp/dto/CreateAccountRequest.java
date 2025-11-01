package com.example.bankingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data // Includes getters and setters for bankId
public class CreateAccountRequest {

    // Removed bankName field

    @NotNull(message = "Bank ID must be provided") // Use ID instead of name
    private Long bankId;
    
    @NotBlank // Make sure it's not empty
    private String nickname;

    // No explicit getters/setters needed if using @Data
}
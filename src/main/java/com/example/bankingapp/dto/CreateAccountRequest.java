package com.example.bankingapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data; // Using Lombok for getters/setters is easier

@Data // Includes getters and setters for bankId
public class CreateAccountRequest {

    // Removed bankName field

    @NotNull(message = "Bank ID must be provided") // Use ID instead of name
    private Long bankId; 

    // No explicit getters/setters needed if using @Data
}
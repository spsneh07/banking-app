package com.example.bankingapp.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data // Lombok: Creates getters, setters, etc.
public class LoginRequest {

    @NotBlank // Validation: Ensures this field is not null or empty
    private String username;

    @NotBlank // Validation: Ensures this field is not null or empty
    private String password;
}
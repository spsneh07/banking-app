package com.example.bankingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PinSetupRequest {

    // We need the username and password to verify *who* is setting the PIN
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;

    @NotBlank
    @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
    private String pin;
}
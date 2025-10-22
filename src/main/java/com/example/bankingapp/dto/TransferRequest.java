package com.example.bankingapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size; // <-- Import this
import lombok.Data;

@Data
public class TransferRequest {

    @NotBlank(message = "Recipient account number cannot be blank")
    private String recipientAccountNumber;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transfer amount must be positive")
    private BigDecimal amount;
    
    // --- THIS IS THE CHANGE ---
    // 1. Removed the 'password' field.
    // 2. Added the 'pin' field to match the requirement.
    @NotBlank(message = "PIN is required for verification")
    @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
    private String pin;
    // --------------------------
}
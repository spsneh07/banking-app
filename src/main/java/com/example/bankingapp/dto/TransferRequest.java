package com.example.bankingapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest {

    // --- CHANGE THIS FIELD ---
    @NotBlank(message = "Recipient account number cannot be blank")
    private String recipientAccountNumber;
    // -----------------------

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transfer amount must be positive")
    private BigDecimal amount;
    
    @NotBlank(message = "Password is required for verification")
    private String password;
}

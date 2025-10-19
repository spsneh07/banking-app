package com.example.bankingapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepositRequest {

    // --- REMOVE The accountId field ---

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Deposit amount must be positive")
    private BigDecimal amount;
}
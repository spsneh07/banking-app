package com.example.bankingapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SelfTransferRequest {

    @NotNull(message = "Source account ID cannot be null")
    private Long sourceAccountId;

    @NotNull(message = "Destination account ID cannot be null")
    private Long destinationAccountId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transfer amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "PIN cannot be empty")
    @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
    private String pin;
}
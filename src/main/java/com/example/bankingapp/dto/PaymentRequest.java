package com.example.bankingapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotBlank(message = "Biller name cannot be blank")
    private String billerName;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be positive")
    private BigDecimal amount;
    
    // --- ADD THIS FIELD ---
    @NotBlank(message = "PIN is required for verification")
    @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
    private String pin;
    // --------------------
}
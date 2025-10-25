package com.example.bankingapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin; // For validation annotations
import jakarta.validation.constraints.NotBlank; // For getters/setters
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data // Automatically adds getters, setters, toString, etc.
public class LoanApplicationRequest {

    @NotNull(message = "Loan amount cannot be null")
    @DecimalMin(value = "1000.00", message = "Loan amount must be at least ₹1,000.00") // Example minimum
    private BigDecimal amount;

    @NotBlank(message = "Loan purpose cannot be empty") // Ensures the string is not null and not just whitespace
    private String purpose;

    @NotNull(message = "Monthly income cannot be null")
    @DecimalMin(value = "0.00", message = "Monthly income cannot be negative")
    private BigDecimal monthlyIncome;
}
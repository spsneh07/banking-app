package com.example.bankingapp.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor; // Required import
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // <-- Keep THIS annotation
public class SpendingByCategoryDto {
    private String category;
    private BigDecimal totalSpent;

    // --- DELETE THIS MANUAL CONSTRUCTOR ---
    /*
    public SpendingByCategoryDto(String category, BigDecimal totalSpent) {
        this.category = category;
        this.totalSpent = totalSpent;
    }
    */
    // --- END DELETE ---
}
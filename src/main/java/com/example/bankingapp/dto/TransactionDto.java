package com.example.bankingapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.bankingapp.model.Transaction;
import com.example.bankingapp.model.TransactionType;

import lombok.Data;

@Data
public class TransactionDto {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    
    // --- 1. ADD THIS FIELD ---
    private String description;
    // -----------------------

    public TransactionDto(Transaction transaction) {
        this.id = transaction.getId();
        this.type = transaction.getType();
        this.amount = transaction.getAmount();
        this.timestamp = transaction.getTimestamp();
        
        // --- 2. ADD THIS MAPPING ---
        this.description = transaction.getDescription();
        // -------------------------
    }
}
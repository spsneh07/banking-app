package com.example.bankingapp.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import com.example.bankingapp.model.Transaction;

import lombok.Data;

/**
 * A DTO for sending transaction details to the frontend.
 */
@Data
public class TransactionDto {
    private String type;
    private BigDecimal amount;
    private String description;
    private String timestamp;

    public TransactionDto(Transaction transaction) {
        this.type = transaction.getType().toString();
        this.amount = transaction.getAmount();
        this.description = transaction.getDescription();
        // Format the timestamp into a more readable string for the UI
        this.timestamp = transaction.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
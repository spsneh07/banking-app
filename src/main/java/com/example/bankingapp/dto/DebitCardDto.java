package com.example.bankingapp.dto;

import java.time.LocalDate;

import com.example.bankingapp.model.DebitCard;

import lombok.Data;

@Data
public class DebitCardDto {

    private String cardHolderName;
    private String cardNumberLastFour; // Only send last 4 digits
    private LocalDate expiryDate;
    private boolean active;
    
    // We will NOT include the full card number or CVV for security.

    public DebitCardDto(DebitCard card) {
        this.cardHolderName = card.getCardHolderName();
        // Get last 4 digits (e.g., from "1234-5678-9012-3456" -> "3456")
        this.cardNumberLastFour = card.getCardNumber().substring(card.getCardNumber().length() - 4);
        this.expiryDate = card.getExpiryDate();
        this.active = card.isActive();
    }
}
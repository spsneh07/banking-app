package com.example.bankingapp.dto;

import com.example.bankingapp.model.DebitCard;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Make sure you have this
public class DebitCardDto {

    // 1. These fields are correct
    private String cardHolderName;
    private boolean active;

    // 2. Add the full cardNumber (this is what your JS expects)
    private String cardNumber;

    // 3. Change expiryDate from LocalDate to String
    private String expiryDate;

    // 4. The constructor now correctly populates these fields
    public DebitCardDto(DebitCard card) {
        this.cardHolderName = card.getCardHolderName();
        this.active = card.isActive();
        
        // This line will now work
        this.cardNumber = card.getCardNumber(); 
        
        // Format the Expiry Date from LocalDate to "MM/YY" string
        if (card.getExpiryDate() != null) {
            this.expiryDate = String.format("%02d/%02d", 
                card.getExpiryDate().getMonthValue(), 
                card.getExpiryDate().getYear() % 100);
        } else {
            this.expiryDate = "MM/YY";
        }
    }
}
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
    private boolean onlineTransactionsEnabled;
    private boolean internationalTransactionsEnabled;
    
// Add these methods if not using Lombok @Getter/@Data
public boolean isOnlineTransactionsEnabled() {
    return onlineTransactionsEnabled;
}

public boolean isInternationalTransactionsEnabled() {
    return internationalTransactionsEnabled;
}

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
        this.onlineTransactionsEnabled = card.isOnlineTransactionsEnabled(); 
    this.internationalTransactionsEnabled = card.isInternationalTransactionsEnabled();
        
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
package com.example.bankingapp.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType; 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor; 

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor 
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO; 
    
    @Column(nullable = true) 
    private String accountNickname;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY is fine here, DTOs handle it.
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    // --- === FIX #1: Changed from LAZY to EAGER === ---
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private DebitCard debitCard;
    
    // --- === FIX #2: Changed from LAZY to EAGER === ---
    @ManyToOne(fetch = FetchType.EAGER) // Relationship with Bank entity (Correct)
    @JoinColumn(name = "bank_id", nullable = false)
    // @JsonIgnore // This isn't needed here, but doesn't hurt
    private Bank bank;

    // Constructor for creating new accounts
    public Account(User user, Bank bank) {
        this.balance = BigDecimal.ZERO;
        // Consider using a more robust way to generate unique account numbers
        long number = 1_000_000_000L + (long) (Math.random() * 9_000_000_000L); 
        this.accountNumber = String.valueOf(number);
        this.user = user;
        this.bank = bank; // Set the Bank entity relationship
    }

    // Helper method to correctly link the card to the account
    public void setDebitCard(DebitCard debitCard) {
        if (debitCard == null) {
            if (this.debitCard != null) {
                this.debitCard.setAccount(null);
            }
        } else {
            debitCard.setAccount(this);
        }
        this.debitCard = debitCard;
    }
}

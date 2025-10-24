package com.example.bankingapp.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType; // <-- Make sure this is imported
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
import lombok.NoArgsConstructor; // <-- 1. ADD THIS IMPORT

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor // <-- 2. ADD THIS ANNOTATION
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private DebitCard debitCard;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    @JsonIgnore
    private Bank bank;

    // This constructor is fine, it will be used by your service
    public Account(User user, Bank bank) {
        this.balance = BigDecimal.ZERO;
        long number = 1_000_000_000L + (long) (Math.random() * 9_000_000_000L);
        this.accountNumber = String.valueOf(number);
        this.user = user;
        this.bank = bank;
    }

    // --- 3. ADD THIS HELPER METHOD ---
    // This correctly links the card to the account
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
    // ---------------------------------
}
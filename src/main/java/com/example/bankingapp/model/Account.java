package com.example.bankingapp.model;

import java.math.BigDecimal;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column; // <<< IMPORT THIS
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "accounts")
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We'll store this as a String to preserve leading zeros, 
    // but it will be all digits.
    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    // --- UPDATED CONSTRUCTOR ---
    public Account() {
        this.balance = BigDecimal.ZERO;
        
        // Generate a 10-digit random number as a String
        Random rand = new Random();
        // Generates a number between 1,000,000,000 and 9,999,999,999
        long number = 1_000_000_000L + rand.nextLong(9_000_000_000L);
        this.accountNumber = String.valueOf(number);
    }
}
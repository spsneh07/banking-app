package com.example.bankingapp.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column; // Import UUID
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

    // --- ADD THIS FIELD ---
    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;
    // --------------------

    @Column(nullable = false)
    private BigDecimal balance;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    // --- UPDATE THE CONSTRUCTOR ---
    public Account() {
        this.balance = BigDecimal.ZERO;
        // Generate a unique, random account number when a new account is created
        this.accountNumber = UUID.randomUUID().toString();
    }
}

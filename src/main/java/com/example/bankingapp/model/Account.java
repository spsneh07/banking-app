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
    private BigDecimal balance = BigDecimal.ZERO; // <-- THIS IS THE FIX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    // REMOVED redundant bankName field
    // @Column(name = "bank_name")
    // private String bankName;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private DebitCard debitCard;
    
    // REMOVED incorrect self-referencing Account relationship
    // @ManyToOne(fetch = FetchType.LAZY) 
    // @JoinColumn(name = "account_id", nullable = true) 
    // private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY) // Relationship with Bank entity (Correct)
    @JoinColumn(name = "bank_id", nullable = false)
    @JsonIgnore
    private Bank bank;

    // REMOVED ActivityLog constructors - they belong in ActivityLog.java
    /* public ActivityLog(User user, Account account, String activityType, String description) { ... } */
    /* public ActivityLog(User user, String activityType, String description) { ... } */

    // Constructor for creating new accounts
    public Account(User user, Bank bank) {
        this.balance = BigDecimal.ZERO;
        // Consider using a more robust way to generate unique account numbers
        long number = 1_000_000_000L + (long) (Math.random() * 9_000_000_000L); 
        this.accountNumber = String.valueOf(number);
        this.user = user;
        this.bank = bank; // Set the Bank entity relationship
    }

    // REMOVED redundant bankName getter/setter
    /* public String getBankName() { ... } */
    /* public void setBankName(String bankName) { ... } */

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
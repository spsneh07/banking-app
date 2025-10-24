package com.example.bankingapp.dto;

import java.math.BigDecimal;

import com.example.bankingapp.model.Account;

import lombok.Data;

@Data
public class AccountDto {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private BankDto bank; // This includes the bank details

    // --- 1. ADD THIS FIELD ---
    private DebitCardDto debitCard;
    // -----------------------

    public AccountDto(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        
        // This is the crucial part that loads the bank info
        if (account.getBank() != null) {
            this.bank = new BankDto(account.getBank()); 
        }

        // --- 2. ADD THIS LOGIC ---
        // Check if the debit card exists before trying to create the DTO
        if (account.getDebitCard() != null) {
            this.debitCard = new DebitCardDto(account.getDebitCard());
        }
        // -----------------------
    }
}
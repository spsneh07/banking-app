package com.example.bankingapp.dto;

import java.math.BigDecimal;

import com.example.bankingapp.model.Account;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class AccountDto {
    private Long id;
   @JsonIgnore
    private String accountNumber;
    private BigDecimal balance;
    private BankDto bank; // This includes the bank details
    private String nickname;

    // --- 1. ADD THIS FIELD ---
    private DebitCardDto debitCard;
    // -----------------------

    public AccountDto(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        this.nickname = account.getAccountNickname();
        
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
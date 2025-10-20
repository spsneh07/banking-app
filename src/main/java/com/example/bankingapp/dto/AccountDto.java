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

    public AccountDto(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        // This is the crucial part that loads the bank info
        this.bank = new BankDto(account.getBank()); 
    }
}
package com.example.bankingapp.dto;

import com.example.bankingapp.model.Bank;

import lombok.Data;

@Data
public class BankDto {
    private Long id;
    private String name;

    public BankDto(Bank bank) {
        this.id = bank.getId();
        this.name = bank.getName();
    }
}

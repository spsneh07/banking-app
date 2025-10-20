package com.example.bankingapp.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountHistoryDto {
    private String date; // "dd-MMM" format
    private BigDecimal balance;
}

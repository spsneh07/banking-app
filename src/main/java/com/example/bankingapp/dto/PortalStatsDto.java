package com.example.bankingapp.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalStatsDto {
    private BigDecimal totalBalance;
    private int linkedBanksCount;
}

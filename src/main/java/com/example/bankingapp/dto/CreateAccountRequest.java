package com.example.bankingapp.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAccountRequest {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    // Getter
    public String getBankName() {
        return bankName;
    }

    // Setter
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
}
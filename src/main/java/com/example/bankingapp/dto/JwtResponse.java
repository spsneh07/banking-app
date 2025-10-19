package com.example.bankingapp.dto;

import lombok.Data;

@Data
public class JwtResponse {

    private String accessToken;
    private String tokenType = "Bearer"; // Standard prefix for JWTs

    // Constructor to easily create this object
    public JwtResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
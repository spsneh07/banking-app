package com.example.bankingapp.dto;

import java.util.List;

import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.User;

import lombok.Data;

@Data
public class UserDto {
    private String username;
    private String fullName;
    private String email;
    
    // This DTO will now include the user's list of accounts
    private List<Account> accounts;

    public UserDto(User user) {
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        
        // --- THIS IS THE FIX ---
        // We now call the plural .getAccounts() method
        this.accounts = user.getAccounts();
        // -----------------------
    }
}


package com.example.bankingapp.dto;

import com.example.bankingapp.model.User;

import lombok.Data;

@Data
public class UserDto {
    private String username;
    private String fullName;
    private String accountNumber;

    public UserDto(User user) {
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        if (user.getAccount() != null) {
            // This line is correct. The getAccountNumber() method
            // is generated automatically by Lombok's @Data annotation
            // on the Account class.
            this.accountNumber = user.getAccount().getAccountNumber();
        }
    }
}


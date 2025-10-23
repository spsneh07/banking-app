package com.example.bankingapp.dto;

import java.time.LocalDate; // <-- ADD THIS IMPORT

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past; // <-- ADD THIS IMPORT
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    
    @NotBlank(message = "Full Name cannot be blank")
    @Size(min = 3, max = 50)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    private String phoneNumber;
    
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth; // e.g., "1990-10-25"
    
    private String address;
    
    private String nomineeName;
}
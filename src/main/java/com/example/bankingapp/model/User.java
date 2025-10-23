package com.example.bankingapp.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;
    
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth; // Use java.time.LocalDate

    @Column(name = "address")
    private String address;

    @Column(name = "nominee_name")
    private String nomineeName;
    
    // --- THIS IS THE CORRECTION ---
    // Merged both pin declarations into one.
    // It maps to the 'user_pin' database column and is allowed to be null.
    @Column(name = "user_pin", nullable = true)
    private String pin;
    // -------------------------------
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Account> accounts = new ArrayList<>();

    public User(String fullName, String username, String email, String password) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
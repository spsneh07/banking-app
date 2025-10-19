package com.example.bankingapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Find an account by the owner's username
    Optional<Account> findByUserUsername(String username);
    
    // --- ADD THIS METHOD ---
    // Find an account by its unique account number
    Optional<Account> findByAccountNumber(String accountNumber);
    // --------------------
}

package com.example.bankingapp.repository;

import java.util.List;
import java.util.Optional; // <<< IMPORT

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserUsername(String username);
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    // --- NEW METHOD to find all accounts for a portal user ---
    List<Account> findByUserUsernameOrderByBankName(String username);
    
    // --- NEW METHOD to check if a user already has an account at a bank ---
    boolean existsByUserUsernameAndBankId(String username, Long bankId);
}
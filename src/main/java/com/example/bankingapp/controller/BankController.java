package com.example.bankingapp.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping; // We need this
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.Bank;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.AccountRepository;
import com.example.bankingapp.repository.BankRepository;
import com.example.bankingapp.repository.UserRepository;
import com.example.bankingapp.service.AccountService;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/banks")
@CrossOrigin(origins = "*")
public class BankController {

    @Autowired private BankRepository bankRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountService accountService; // For deposit

    // This method runs once on startup to create your banks
   // ... (keep all your @Autowired injections) ...

    // This method runs once on startup to create your banks
    @PostConstruct
    public void createInitialBanks() {
        if (bankRepository.count() == 0) {
            bankRepository.save(new Bank("ICICI Bank"));
            bankRepository.save(new Bank("HDFC Bank"));
            bankRepository.save(new Bank("IDBI Bank"));
            bankRepository.save(new Bank("Union Bank of India"));
            // You can add more banks here
        }
    }

    // ... (keep the rest of the file: getAllBanks, addBankToUser, etc.) ...

    // Get all banks available in the system
    @GetMapping("/all")
    public ResponseEntity<List<Bank>> getAllBanks() {
        return ResponseEntity.ok(bankRepository.findAll());
    }

    // "Open an account" / Add a bank to the user's portal
    @PostMapping("/add/{bankId}")
    public ResponseEntity<?> addBankToUser(@PathVariable Long bankId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 1. Check if user and bank exist
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Bank bank = bankRepository.findById(bankId)
            .orElseThrow(() -> new RuntimeException("Bank not found"));

        // 2. Check if user already has an account at this bank
        if (accountRepository.existsByUserUsernameAndBankId(username, bankId)) {
            return ResponseEntity.badRequest().body("You already have an account with " + bank.getName());
        }

        // 3. Create a new account for this user at this bank
        Account newAccount = new Account();
        newAccount.setUser(user);
        newAccount.setBank(bank);
        // We'll give them a $50 bonus for opening a new account!
        newAccount.setBalance(new BigDecimal("50.00")); 
        
        accountRepository.save(newAccount);
        
        // Also create a transaction for the bonus
        accountService.createInitialDepositTransaction(newAccount);

        return ResponseEntity.ok(newAccount);
    }
}
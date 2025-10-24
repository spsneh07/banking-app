package com.example.bankingapp.controller;

import java.math.BigDecimal;
import java.time.LocalDate; // <-- 1. ADD IMPORT
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.Bank;
import com.example.bankingapp.model.DebitCard; // <-- 1. ADD IMPORT
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.AccountRepository;
import com.example.bankingapp.repository.BankRepository;
import com.example.bankingapp.repository.DebitCardRepository; // <-- 1. ADD IMPORT
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
    @Autowired private AccountService accountService;

    // --- 2. INJECT THE NEW REPOSITORY ---
    @Autowired private DebitCardRepository debitCardRepository;
    // ------------------------------------

    @PostConstruct
    public void createInitialBanks() {
        if (bankRepository.count() == 0) {
            bankRepository.save(new Bank("ICICI Bank"));
            bankRepository.save(new Bank("HDFC Bank"));
            bankRepository.save(new Bank("IDBI Bank"));
            bankRepository.save(new Bank("Union Bank of India"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Bank>> getAllBanks() {
        return ResponseEntity.ok(bankRepository.findAll());
    }

    // --- 3. UPDATED THIS METHOD ---
    @PostMapping("/add/{bankId}")
    public ResponseEntity<?> addBankToUser(@PathVariable Long bankId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Bank not found"));

        if (accountRepository.existsByUserUsernameAndBankId(username, bankId)) {
            return ResponseEntity.badRequest().body("You already have an account with " + bank.getName());
        }

        // Create new account
        Account newAccount = new Account(user, bank); // Using the constructor
        // Set balance (bonus)
        newAccount.setBalance(new BigDecimal("50.00")); 
        
        // Save the account *first* to get an ID
        accountRepository.save(newAccount);
        
        // --- ADDED DEBIT CARD LOGIC ---
        // 4. Create a new Debit Card for this account
        DebitCard newCard = generateDebitCard(newAccount, user.getFullName());
        debitCardRepository.save(newCard); // Save the card

        // 5. Link the card to the account using the helper method
        newAccount.setDebitCard(newCard);
        accountRepository.save(newAccount); // Re-save the account with the card link
        // --------------------------------

        // Also create a transaction for the bonus
        accountService.createInitialDepositTransaction(newAccount);

        return ResponseEntity.ok(newAccount); // newAccount now contains the card
    }

    // --- 4. ADDED THIS HELPER METHOD ---
    private DebitCard generateDebitCard(Account account, String cardHolderName) {
        // Generate random card number (16 digits in 4 blocks)
        String cardNumber = String.format("%04d-%04d-%04d-%04d",
                (long) (Math.random() * 10000),
                (long) (Math.random() * 10000),
                (long) (Math.random() * 10000),
                (long) (Math.random() * 10000)
        );
        
        // Generate random 3-digit CVV
        String cvv = String.format("%03d", (int) (Math.random() * 1000));
        
        // Set expiry date (e.g., 5 years from now)
        LocalDate expiryDate = LocalDate.now().plusYears(5);
        
        return new DebitCard(cardNumber, cardHolderName, expiryDate, cvv, account);
    }
    // -------------------------------------
}
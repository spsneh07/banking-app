package com.example.bankingapp.controller;

import com.example.bankingapp.dto.*;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.UserRepository;
import com.example.bankingapp.service.AccountService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired private AccountService accountService;
    @Autowired private UserRepository userRepository;

    // --- NEW: Get current user details ---
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserDetails() {
        try {
            User user = userRepository.findByUsername(getAuthenticatedUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(new UserDto(user));
        } catch (Exception e) {
            logger.error("Failed to fetch user details for {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.status(500).body("Error fetching user details.");
        }
    }
    
    // --- NEW: Update user profile ---
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        try {
            User updatedUser = accountService.updateUserProfile(getAuthenticatedUsername(), request.getFullName(), request.getEmail());
            return ResponseEntity.ok(new UserDto(updatedUser));
        } catch (Exception e) {
            logger.error("Profile update failed for user {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // --- NEW: Change user password ---
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            accountService.changeUserPassword(getAuthenticatedUsername(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok("Password changed successfully.");
        } catch (Exception e) {
            logger.error("Password change failed for user {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // --- Existing Transaction Endpoints ---
    @PostMapping("/deposit")
    public ResponseEntity<?> makeDeposit(@Valid @RequestBody DepositRequest depositRequest) {
        try {
            accountService.deposit(getAuthenticatedUsername(), depositRequest.getAmount()); 
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            logger.error("Deposit failed for user {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<?> makeTransfer(@Valid @RequestBody TransferRequest transferRequest) {
        try {
            accountService.transfer(getAuthenticatedUsername(), transferRequest.getRecipientAccountNumber(), transferRequest.getAmount(), transferRequest.getPassword());
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            logger.error("Transfer failed for user {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/paybill")
    public ResponseEntity<?> payBill(@Valid @RequestBody PaymentRequest paymentRequest) {
        try {
            accountService.payBill(getAuthenticatedUsername(), paymentRequest.getBillerName(), paymentRequest.getAmount(), paymentRequest.getPassword());
            return ResponseEntity.ok("Payment successful");
        } catch (Exception e) {
            logger.error("Bill payment failed for user {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getAccountBalance() {
         try {
             return ResponseEntity.ok(accountService.getBalance(getAuthenticatedUsername()));
         } catch (Exception e) {
             logger.error("Failed to get balance for user {}: {}", getAuthenticatedUsername(), e.getMessage());
             return ResponseEntity.status(500).body("Error fetching balance");
         }
     }
     
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions() {
        try {
            return ResponseEntity.ok(accountService.getRecentTransactions(getAuthenticatedUsername()));
        } catch (Exception e) {
            logger.error("Failed to fetch transactions for user {}: {}", getAuthenticatedUsername(), e.getMessage());
            return ResponseEntity.status(500).body("Error fetching transactions");
        }
    }
    
    @GetMapping("/verify-recipient")
    public ResponseEntity<?> verifyRecipient(@RequestParam String accountNumber) {
        try {
            return ResponseEntity.ok(accountService.verifyRecipient(accountNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found in security context");
        }
        return authentication.getName();
    }
}


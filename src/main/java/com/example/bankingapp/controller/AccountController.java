package com.example.bankingapp.controller;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingapp.dto.DepositRequest;
import com.example.bankingapp.dto.PaymentRequest;
import com.example.bankingapp.dto.TransactionDto;
import com.example.bankingapp.dto.TransferRequest;
import com.example.bankingapp.dto.UserDto;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.UserRepository;
import com.example.bankingapp.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired private AccountService accountService;
    @Autowired private UserRepository userRepository; // Inject UserRepository for /me endpoint

    @PostMapping("/deposit")
    public ResponseEntity<?> makeDeposit(@Valid @RequestBody DepositRequest depositRequest) {
        try {
            accountService.deposit(getAuthenticatedUsername(), depositRequest.getAmount()); 
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<?> makeTransfer(@Valid @RequestBody TransferRequest transferRequest) {
        try {
            accountService.transfer(
                getAuthenticatedUsername(), 
                transferRequest.getRecipientAccountNumber(), // Use account number
                transferRequest.getAmount(),
                transferRequest.getPassword()
            );
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/paybill")
    public ResponseEntity<?> payBill(@Valid @RequestBody PaymentRequest paymentRequest) {
        try {
            accountService.payBill(
                getAuthenticatedUsername(), 
                paymentRequest.getBillerName(), 
                paymentRequest.getAmount(),
                paymentRequest.getPassword()
            );
            return ResponseEntity.ok("Payment successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getAccountBalance() {
         try {
             BigDecimal balance = accountService.getBalance(getAuthenticatedUsername());
             return ResponseEntity.ok(balance);
         } catch (Exception e) {
             return ResponseEntity.status(500).body("Error fetching balance");
         }
     }
     
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions() {
        try {
            List<TransactionDto> transactions = accountService.getRecentTransactions(getAuthenticatedUsername());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching transactions");
        }
    }
    
    @GetMapping("/verify-recipient")
    public ResponseEntity<?> verifyRecipient(@RequestParam String accountNumber) { // <<< LOOKUP BY ACCOUNT NUMBER
        try {
            String fullName = accountService.verifyRecipient(accountNumber);
            return ResponseEntity.ok(fullName);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // --- NEW ENDPOINT TO GET CURRENT USER DETAILS ---
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserDetails() {
        try {
            User user = userRepository.findByUsername(getAuthenticatedUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(new UserDto(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching user details.");
        }
    }
    
    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

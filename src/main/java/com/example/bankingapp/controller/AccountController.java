package com.example.bankingapp.controller;

import java.io.IOException; // <-- IMPORT
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingapp.dto.AccountDto;
import com.example.bankingapp.dto.DepositRequest;
import com.example.bankingapp.dto.PasswordChangeRequest;
import com.example.bankingapp.dto.PaymentRequest;
import com.example.bankingapp.dto.PinSetupRequest;
import com.example.bankingapp.dto.ProfileUpdateRequest;
import com.example.bankingapp.dto.TransferRequest;
import com.example.bankingapp.dto.UserDto;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.UserRepository;
import com.example.bankingapp.service.AccountService;
import com.example.bankingapp.service.CsvExportService; // <-- IMPORT

import jakarta.servlet.http.HttpServletResponse; // <-- IMPORT
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired private AccountService accountService;
    @Autowired private UserRepository userRepository;
    @Autowired private com.example.bankingapp.repository.AccountRepository accountRepository;

    @Autowired private CsvExportService csvExportService; // <-- INJECT NEW SERVICE

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found in security context");
        }
        return authentication.getName();
    }
    
    private void verifyAccountOwner(Long accountId) {
        String username = getAuthenticatedUsername();
        com.example.bankingapp.model.Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!account.getUser().getUsername().equals(username)) {
            throw new IllegalStateException("User does not own this account");
        }
    }

    // --- THIS IS THE SINGLE, CORRECT /all ENDPOINT ---
    @GetMapping("/all")
    public ResponseEntity<?> getAllUserAccounts() {
        try {
            // Call the service method that returns DTOs with all the needed data
            List<AccountDto> accounts = accountService.getAccountsForUser(getAuthenticatedUsername());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching accounts");
        }
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<?> makeDeposit(@PathVariable Long accountId, @Valid @RequestBody DepositRequest depositRequest) {
        try {
            verifyAccountOwner(accountId);
            accountService.deposit(getAuthenticatedUsername(), accountId, depositRequest.getAmount()); 
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<?> makeTransfer(@PathVariable Long accountId, @Valid @RequestBody TransferRequest transferRequest) {
        try {
            verifyAccountOwner(accountId);
            accountService.transfer(
                getAuthenticatedUsername(), 
                accountId,
                transferRequest.getRecipientAccountNumber(),
                transferRequest.getAmount(),
                transferRequest.getPin() // --- MODIFIED --- (was getPassword())
            );
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/{accountId}/paybill")
    public ResponseEntity<?> payBill(@PathVariable Long accountId, @Valid @RequestBody PaymentRequest paymentRequest) {
        try {
            verifyAccountOwner(accountId);
            accountService.payBill(
                getAuthenticatedUsername(), 
                accountId,
                paymentRequest.getBillerName(), 
                paymentRequest.getAmount(),
                paymentRequest.getPin() // --- MODIFIED --- 
            );
            return ResponseEntity.ok("Payment successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getAccountBalance(@PathVariable Long accountId) {
         try {
             verifyAccountOwner(accountId);
             return ResponseEntity.ok(accountService.getBalance(accountId));
         } catch (Exception e) {
             return ResponseEntity.status(500).body("Error fetching balance");
         }
     }
     
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<?> getTransactions(@PathVariable Long accountId) {
        try {
            verifyAccountOwner(accountId);
            return ResponseEntity.ok(accountService.getRecentTransactions(accountId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching transactions");
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserDetails() {
        try {
            User user = userRepository.findByUsername(getAuthenticatedUsername()).orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(new UserDto(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching user details.");
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        try {
            User updatedUser = accountService.updateUserProfile(getAuthenticatedUsername(), request.getFullName(), request.getEmail());
            return ResponseEntity.ok(new UserDto(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            accountService.changeUserPassword(getAuthenticatedUsername(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok("Password changed successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/set-pin")
    public ResponseEntity<?> setPin(@Valid @RequestBody PinSetupRequest request) {
        try {
            accountService.setPin(getAuthenticatedUsername(), request.getPassword(), request.getPin());
            return ResponseEntity.ok("PIN set successfully."); 
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password. Please try again.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

    // --- NEW ENDPOINT FOR CSV DOWNLOAD ---
    @GetMapping("/{accountId}/export/csv")
    public void exportTransactionsToCsv(@PathVariable Long accountId, HttpServletResponse response) {
        try {
            // 1. Verify the user owns this account
            verifyAccountOwner(accountId);
            String username = getAuthenticatedUsername();

            // 2. Set HTTP headers for file download
            response.setContentType("text/csv");
            String headerKey = "Content-Disposition";
            // Create a dynamic filename like "statement-12345.csv"
            String headerValue = "attachment; filename=\"statement-" + accountId + ".csv\"";
            response.setHeader(headerKey, headerValue);

            // 3. Call the service to write CSV data directly to the response
            csvExportService.writeTransactionsToCsv(response.getWriter(), username, accountId);

        } catch (IOException e) {
            // Handle IO exception
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } catch (Exception e) {
            // Handle other exceptions (like security verification failure)
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
    }
    // ------------------------------------
}
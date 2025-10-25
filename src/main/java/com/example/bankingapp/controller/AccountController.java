package com.example.bankingapp.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random; // <-- ADD THIS LINE

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
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
import com.example.bankingapp.dto.CreateAccountRequest;
import com.example.bankingapp.dto.DebitCardDto;
import com.example.bankingapp.dto.DepositRequest;
import com.example.bankingapp.dto.PasswordChangeRequest;
import com.example.bankingapp.dto.PaymentRequest;
import com.example.bankingapp.dto.PinSetupRequest;
import com.example.bankingapp.dto.ProfileUpdateRequest;
import com.example.bankingapp.dto.TransferRequest;
import com.example.bankingapp.dto.UserDto;
import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.DebitCard;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.DebitCardRepository;
import com.example.bankingapp.repository.UserRepository;
import com.example.bankingapp.service.AccountService;
import com.example.bankingapp.service.CsvExportService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired private AccountService accountService;
    @Autowired private UserRepository userRepository;
    @Autowired private DebitCardRepository debitCardRepository;
    @Autowired private com.example.bankingapp.repository.AccountRepository accountRepository;

    @Autowired private CsvExportService csvExportService; 

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

    @GetMapping("/all")
    public ResponseEntity<?> getAllUserAccounts() {
        try {
            List<AccountDto> accounts = accountService.getAccountsForUser(getAuthenticatedUsername());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching accounts");
        }
    }
    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            // 1. Find the logged-in user
            String username = getAuthenticatedUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Create the new Account
            Account newAccount = new Account();
            newAccount.setUser(user);
            newAccount.setBankName(request.getBankName());
            newAccount.setAccountNumber(generateRandomAccountNumber());
            newAccount.setBalance(new BigDecimal("50.00")); // Initial bonus
            
            Account savedAccount = accountRepository.save(newAccount);

            // 3. Create the initial deposit transaction
            accountService.createInitialDepositTransaction(savedAccount);

            // 4. Create the new Debit Card
            DebitCard newCard = new DebitCard();
            newCard.setCardHolderName(user.getFullName());
            newCard.setAccount(savedAccount); // <-- Link to the new account
            newCard.setActive(true);
            newCard.setCardNumber(generateRandomCardNumber());
            newCard.setCvv(generateRandomCvv());
            newCard.setExpiryDate(LocalDate.now().plusYears(5)); 
            
            debitCardRepository.save(newCard); // Save the card!

            return ResponseEntity.ok(new AccountDto(savedAccount));
        
       } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating account: " + e.getMessage());
       }
    }

@PostMapping("/{accountId}/deposit")
    public ResponseEntity<?> makeDeposit(@PathVariable Long accountId, @Valid @RequestBody DepositRequest depositRequest) {
        try {
            verifyAccountOwner(accountId);
            // --- MODIFIED ---
            // Now pass the source to the service
            accountService.deposit(
                getAuthenticatedUsername(), 
                accountId, 
                depositRequest.getAmount(), 
                depositRequest.getSource()
            ); 
            // ------------------
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
                transferRequest.getPin()
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
                paymentRequest.getPin()
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
    
    // --- MODIFIED THIS METHOD ---
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        try {
            // Now we pass the entire 'request' object to the service
            User updatedUser = accountService.updateUserProfile(getAuthenticatedUsername(), request);
            return ResponseEntity.ok(new UserDto(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // --- END OF MODIFICATION ---
    
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
        String username = getAuthenticatedUsername();
        try {
            accountService.setPin(username, request.getPassword(), request.getPin());
            return ResponseEntity.ok("PIN set successfully.");
        } catch (BadCredentialsException e) { 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid current password.");
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed.");
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

    @GetMapping("/{accountId}/export/csv")
    public void exportTransactionsToCsv(@PathVariable Long accountId, HttpServletResponse response) {
        try {
            verifyAccountOwner(accountId);
            String username = getAuthenticatedUsername();

            response.setContentType("text/csv");
            String headerKey = "Content-Disposition";
            String headerValue = "attachment; filename=\"statement-" + accountId + ".csv\"";
            response.setHeader(headerKey, headerValue);

            csvExportService.writeTransactionsToCsv(response.getWriter(), username, accountId);

        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
    }
    @GetMapping("/{accountId}/card")
    public ResponseEntity<?> getDebitCardDetails(@PathVariable Long accountId) {
        try {
            verifyAccountOwner(accountId);
            String username = getAuthenticatedUsername();
            DebitCardDto cardDto = accountService.getDebitCardDetails(accountId, username);
            return ResponseEntity.ok(cardDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

 @PostMapping("/{accountId}/card/toggle")
    public ResponseEntity<?> toggleCardStatus(@PathVariable Long accountId) {
        try {
            verifyAccountOwner(accountId);
            String username = getAuthenticatedUsername();
            // --- CHANGE THIS LINE ---
            DebitCardDto updatedCardDto = accountService.toggleDebitCardOption(accountId, username, "master"); // Use new method and add "master"
            // ----------------------
            return ResponseEntity.ok(updatedCardDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Inside AccountController.java

// ... (other methods like /card/cvv, /card/toggle etc.)

@PostMapping("/{accountId}/card/online-toggle") // CHECK THIS PATH
public ResponseEntity<?> toggleOnlineStatus(@PathVariable Long accountId) {
    try {
        verifyAccountOwner(accountId);
        String username = getAuthenticatedUsername();
        DebitCardDto updatedCardDto = accountService.toggleDebitCardOption(accountId, username, "online");
        return ResponseEntity.ok(updatedCardDto);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

@PostMapping("/{accountId}/card/international-toggle") // CHECK THIS PATH
public ResponseEntity<?> toggleInternationalStatus(@PathVariable Long accountId) {
    try {
        verifyAccountOwner(accountId);
        String username = getAuthenticatedUsername();
        DebitCardDto updatedCardDto = accountService.toggleDebitCardOption(accountId, username, "international");
        return ResponseEntity.ok(updatedCardDto);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
    // Inside AccountController.java (around line 250)

// --- NEW CVV ENDPOINT ---
@GetMapping("/{accountId}/card/cvv")
public ResponseEntity<?> getCardCvv(@PathVariable Long accountId) {
    try {
        verifyAccountOwner(accountId);
        String username = getAuthenticatedUsername();
        // Call service method to get CVV (string or DTO)
        String cvv = accountService.getDebitCardCvv(accountId, username); 
        // NOTE: We don't send the full CardDTO, only the CVV as an object.
        return ResponseEntity.ok(Map.of("cvv", cvv)); 
    } catch (Exception e) {
        // Log sensitive error details, but send generic bad request to frontend
        return ResponseEntity.badRequest().body("CVV retrieval failed.");
    }
}
// -------------------------
    // ---------------------------------
    private String generateRandomAccountNumber() {
        Random random = new Random();
        long number = 1_000_000_000L + random.nextInt(900_000_000); // 10-digit number
        return String.valueOf(number);
    }

    private String generateRandomCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append("4200"); // Example prefix
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString(); // 16-digit number
    }

    private String generateRandomCvv() {
        Random random = new Random();
        int cvv = 100 + random.nextInt(900); // 3-digit CVV
        return String.valueOf(cvv);
    }
}

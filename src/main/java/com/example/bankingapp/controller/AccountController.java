package com.example.bankingapp.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model; // This import is used by /dashboard
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
import com.example.bankingapp.dto.ActivityLogDto;
import com.example.bankingapp.dto.CreateAccountRequest;
import com.example.bankingapp.dto.DebitCardDto;
import com.example.bankingapp.dto.DepositRequest;
import com.example.bankingapp.dto.LoanApplicationRequest;
import com.example.bankingapp.dto.PasswordChangeRequest;
import com.example.bankingapp.dto.PaymentRequest;
import com.example.bankingapp.dto.PinSetupRequest;
import com.example.bankingapp.dto.ProfileUpdateRequest;
import com.example.bankingapp.dto.SelfTransferRequest;
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
    @Autowired private com.example.bankingapp.repository.BankRepository bankRepository;
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

    @PostMapping("/self-transfer")
    public ResponseEntity<?> performSelfTransfer(@Valid @RequestBody SelfTransferRequest request) {
        try {
            String username = getAuthenticatedUsername();
            accountService.selfTransfer(
                username,
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getPin()
            );
            return ResponseEntity.ok("Self-transfer successful.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid PIN.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred during the transfer.");
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            String username = getAuthenticatedUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            Long requestedBankId = request.getBankId();
            if (requestedBankId == null) {
                throw new IllegalArgumentException("Bank ID must be provided to create an account.");
            }
            com.example.bankingapp.model.Bank bankEntity = bankRepository.findById(requestedBankId)
                   .orElseThrow(() -> new RuntimeException("Bank not found with ID: " + requestedBankId));
            
            Account newAccount = new Account(user, bankEntity); 
            newAccount.setAccountNumber(generateRandomAccountNumber());
            newAccount.setBalance(new BigDecimal("50.00"));
            
            newAccount.setAccountNickname(request.getNickname());
            Account savedAccount = accountRepository.save(newAccount);

            accountService.createInitialDepositTransaction(savedAccount);

            DebitCard newCard = new DebitCard();
            newCard.setCardHolderName(user.getFullName().toUpperCase());
            newCard.setAccount(savedAccount);
            newCard.setActive(true);
            newCard.setOnlineTransactionsEnabled(true);
            newCard.setInternationalTransactionsEnabled(false);
            newCard.setCardNumber(generateRandomCardNumber());
            newCard.setCvv(generateRandomCvv());
            newCard.setExpiryDate(LocalDate.now().plusYears(4));
            
            DebitCard savedCard = debitCardRepository.save(newCard); 

            savedAccount.setDebitCard(savedCard); 
            accountRepository.save(savedAccount);

            return ResponseEntity.ok(new AccountDto(savedAccount));
        
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating account: " + e.getMessage());
        }
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<?> makeDeposit(@PathVariable Long accountId, @Valid @RequestBody DepositRequest depositRequest) {
        try {
            verifyAccountOwner(accountId);
            accountService.deposit(
                getAuthenticatedUsername(), 
                accountId, 
                depositRequest.getAmount(), 
                depositRequest.getSource()
            ); 
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    

    // --- THIS IS THE CORRECTED ENDPOINT ---
    /**
     * Fetches just the balance for a specific account.
     * This is the SINGLE correct method for this endpoint.
     * It includes the security check and returns only the balance (BigDecimal)
     * to prevent the StackOverflowError.
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long accountId) {
        try {
            // 1. Verify the authenticated user owns this account
            verifyAccountOwner(accountId);
    
            // 2. Call the service method that returns *only* the balance
            // (This assumes accountService.getBalance(id) returns BigDecimal)
            BigDecimal balance = accountService.getBalance(accountId);
    
            // 3. Return the balance as a simple number
            return ResponseEntity.ok(balance);
    
        } catch (IllegalStateException e) {
            // This catches the verifyAccountOwner failure
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    
        } catch (Exception e) {
            // This catches other errors (e.g., account not found from service)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    // --- END OF FIX ---
    

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
    

    @PostMapping("/user/deactivate")
    public ResponseEntity<?> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> payload) {

        String password = payload.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.status(400).body(Map.of("message", "Password is required."));
        }

        try {
            accountService.verifyPasswordAndDeactivate(userDetails.getUsername(), password);
            return ResponseEntity.ok(Map.of("message", "Account deactivated successfully."));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect password. Deactivation cancelled."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "An unexpected error occurred: " + e.getMessage()));
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
            User updatedUser = accountService.updateUserProfile(getAuthenticatedUsername(), request);
            return ResponseEntity.ok(new UserDto(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Model model) {
        // THIS IS TEMPORARY: Replace "testuser" with a username you have
        // registered in your database.
        User user = accountService.findByUsername("testuser"); 

        if (user == null) {
            return "redirect:/login"; // or redirect to register
        }
        
        // This adds the user object to the page
        model.addAttribute("user", user);
        return "dashboard";
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

    @GetMapping("/{accountId}/activity-log")
    public ResponseEntity<?> getAccountActivityLog(@PathVariable Long accountId) {
        try {
            String username = getAuthenticatedUsername(); 
            List<ActivityLogDto> logs = accountService.getActivityLogsForAccount(accountId, username); 
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            System.err.println("Error fetching activity log for account " + accountId + ": " + e.getMessage()); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Error retrieving activity log.");
        }
    }

    @PostMapping("/{accountId}/card/toggle")
    public ResponseEntity<?> toggleCardStatus(@PathVariable Long accountId) {
        try {
            verifyAccountOwner(accountId);
            String username = getAuthenticatedUsername();
            DebitCardDto updatedCardDto = accountService.toggleDebitCardOption(accountId, username, "master");
            return ResponseEntity.ok(updatedCardDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/deactivate-account")
    public String deactivateAccount(@RequestParam("id") Long id) {
        accountService.deactivateUser(id);
        return "redirect:/account-deactivated";
    }
    
    @GetMapping("/account-deactivated")
    public String showDeactivatedPage() {
        return "account_deactivated";
    }

    @PostMapping("/{accountId}/card/online-toggle")
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

    @PostMapping("/{accountId}/card/international-toggle")
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

    @GetMapping("/{accountId}/card/cvv")
    public ResponseEntity<?> getCardCvv(@PathVariable Long accountId) {
        try {
            verifyAccountOwner(accountId);
            String username = getAuthenticatedUsername();
            String cvv = accountService.getDebitCardCvv(accountId, username); 
            return ResponseEntity.ok(Map.of("cvv", cvv)); 
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("CVV retrieval failed.");
        }
    }

    @PostMapping("/{accountId}/loans/apply")
    public ResponseEntity<?> applyForLoan(
            @PathVariable Long accountId,
            @Valid @RequestBody LoanApplicationRequest request
    ) {
        try {
            String username = getAuthenticatedUsername();
            accountService.submitLoanApplication(username, accountId, request); 
            return ResponseEntity.ok("Loan application submitted successfully."); 
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error submitting loan application: " + e.getMessage());
        }
    }

    // --- Private Helper Methods ---
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
package com.example.bankingapp.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingapp.dto.AccountDto;
import com.example.bankingapp.dto.ActivityLogDto;
import com.example.bankingapp.dto.DebitCardDto;
import com.example.bankingapp.dto.LoanApplicationRequest;
import com.example.bankingapp.dto.ProfileUpdateRequest;
import com.example.bankingapp.dto.TransactionDto;
import com.example.bankingapp.model.Account; // Import ActivityLog
import com.example.bankingapp.model.ActivityLog;
import com.example.bankingapp.model.DebitCard;
import com.example.bankingapp.model.Transaction;
import com.example.bankingapp.model.TransactionType;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.AccountRepository; // Import ActivityLogRepository
import com.example.bankingapp.repository.ActivityLogRepository;
import com.example.bankingapp.repository.DebitCardRepository;
import com.example.bankingapp.repository.TransactionRepository;
import com.example.bankingapp.repository.UserRepository;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DebitCardRepository debitCardRepository;
    @Autowired private ActivityLogRepository activityLogRepository; // Injected repository

 @Transactional
    public User updateUserProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (!existingUser.getUsername().equals(username)) {
                throw new IllegalArgumentException("Email is already in use by another account.");
            }
        });

        // Set all new fields
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setNomineeName(request.getNomineeName());
        
        User savedUser = userRepository.save(user); // Save the user first

        // --- UPDATED LOGGING BLOCK (Always logs) ---
        try {
            logger.info("Attempting to save profile update activity log for user: {}", username); // Log before save
            ActivityLog logEntry = new ActivityLog(savedUser, "PROFILE_UPDATE", "Updated profile details (Name/Email/Phone/etc.)");
            activityLogRepository.save(logEntry);
            logger.info("Successfully saved profile update activity log for user: {}", username); // Log after save
        } catch (Exception e) {
            // Log any error that occurs specifically during the activity log saving
            logger.error("Error saving profile update activity log for user {}: {}", username, e.getMessage(), e);
            // Optionally re-throw or handle the error, but for now, just log it.
            // The profile update itself was already saved successfully before this block.
        }
        // --- END LOGGING BLOCK ---

        return savedUser; // Return the saved user
    }

    @Transactional
    public void changeUserPassword(String username, String currentPassword, String newPassword) {
        verifyUserPassword(username, currentPassword);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // --- ADD LOGGING ---
        ActivityLog logEntry = new ActivityLog(user, "PASSWORD_CHANGE", "Changed account password");
        activityLogRepository.save(logEntry);
        // --- END LOGGING ---
    }

    @Transactional
    public void setPin(String username, String currentPassword, String newPin) {
        verifyUserPassword(username, currentPassword);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Check if PIN was previously null to adjust log message
        boolean wasPinSet = user.getPin() != null;
        user.setPin(passwordEncoder.encode(newPin));
        userRepository.save(user);

        // --- ADD LOGGING ---
        String description = wasPinSet ? "Updated security PIN" : "Set initial security PIN";
        ActivityLog logEntry = new ActivityLog(user, "PIN_CHANGE", description);
        activityLogRepository.save(logEntry);
        // --- END LOGGING ---
    }

    // --- Transaction methods (No logging added here as per request) ---
    @Transactional
    public void deposit(String username, Long accountId, BigDecimal amount, String source) {
        Account account = findAccountByIdAndUsername(accountId, username);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        Transaction transaction = new Transaction(TransactionType.DEPOSIT, amount, source, account); 
        transactionRepository.save(transaction);
    }

    @Transactional
    public void transfer(String senderUsername, Long senderAccountId, String recipientAccountNumber, BigDecimal amount, String providedPin) {
        verifyUserPin(senderUsername, providedPin); 
        Account senderAccount = findAccountByIdAndUsername(senderAccountId, senderUsername);
        Account recipientAccount = accountRepository.findByAccountNumber(recipientAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Recipient account number not found."));

        if (senderAccount.getId().equals(recipientAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer money to the same account.");
        }
        if (senderAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for transfer.");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        Transaction senderTx = new Transaction(TransactionType.TRANSFER, amount.negate(), "Transfer to " + recipientAccount.getUser().getFullName(), senderAccount);
        Transaction recipientTx = new Transaction(TransactionType.TRANSFER, amount, "Transfer from " + senderAccount.getUser().getFullName(), recipientAccount);

        transactionRepository.save(senderTx);
        transactionRepository.save(recipientTx);
    }

    @Transactional
    public void payBill(String username, Long accountId, String billerName, BigDecimal amount, String providedPin) {
        verifyUserPin(username, providedPin);
        Account account = findAccountByIdAndUsername(accountId, username);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for bill payment.");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Transaction transaction = new Transaction(TransactionType.PAYMENT, amount.negate(), "Paid bill to " + billerName, account);
        transactionRepository.save(transaction);
    }

    // --- Read-only methods ---
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
         return accountRepository.findById(accountId)
                 .map(Account::getBalance)
                 .orElseThrow(() -> new RuntimeException("Account not found."));
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDto> getRecentTransactions(Long accountId) {
        return transactionRepository.findTop10ByAccountIdOrderByTimestampDesc(accountId)
                .stream().map(TransactionDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String verifyRecipient(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Recipient account number not found."));
        return account.getUser().getFullName();
    }
    
    @Transactional
    public void createInitialDepositTransaction(Account account) {
        Transaction transaction = new Transaction(
            TransactionType.DEPOSIT, 
            new BigDecimal("50.00"), 
            "New account sign-up bonus", 
            account
        );
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsForUser(String username) {
        List<Account> accounts = accountRepository.findByUserUsernameOrderByBankName(username);
        return accounts.stream()
                       .map(AccountDto::new)
                       .collect(Collectors.toList());
    }
    
    // --- Helper methods ---
    private Account findAccountByIdAndUsername(Long accountId, String username) {
        return accountRepository.findById(accountId)
                .filter(account -> account.getUser().getUsername().equals(username))
                .orElseThrow(() -> new RuntimeException("Account not found or user does not own this account."));
    }
    
    private void verifyUserPassword(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid password. Transaction authorization failed.");
        }
    }

    private void verifyUserPin(String username, String providedPin) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (user.getPin() == null) {
            logger.warn("User '{}' attempted a transaction without a PIN set.", username);
            throw new BadCredentialsException("PIN not set. Please create a PIN in your profile.");
        }

        if (!passwordEncoder.matches(providedPin, user.getPin())) {
            logger.warn("Invalid PIN attempt for user '{}'.", username);
            throw new BadCredentialsException("Invalid PIN. Transaction authorization failed.");
        }
    }

    // --- Card related methods ---
    @Transactional(readOnly = true)
    public DebitCardDto getDebitCardDetails(Long accountId, String username) {
        Account account = findAccountByIdAndUsername(accountId, username);
        if (account.getDebitCard() == null) {
            // Consider returning an empty DTO or a specific response instead of throwing an exception
            // depending on how the frontend should handle accounts without cards.
            throw new RuntimeException("No debit card found for this account."); 
        }
        return new DebitCardDto(account.getDebitCard());
    }

    @Transactional(readOnly = true)
    public String getDebitCardCvv(Long accountId, String username) {
        Account account = findAccountByIdAndUsername(accountId, username);
        if (account.getDebitCard() == null) {
            throw new RuntimeException("No debit card found for this account.");
        }
        return account.getDebitCard().getCvv(); 
    }

    @Transactional
    public DebitCardDto toggleDebitCardOption(Long accountId, String username, String option) {
        Account account = findAccountByIdAndUsername(accountId, username);
        DebitCard card = account.getDebitCard();
        if (card == null) {
            throw new RuntimeException("No debit card found for this account.");
        }
        
        String description = ""; 
        boolean previousStatus; 
        String activityType = "CARD_SETTINGS_UPDATE";

        switch (option.toLowerCase()) {
            case "master":
                previousStatus = card.isActive(); 
                card.setActive(!previousStatus);
                description = previousStatus ? "Froze Debit Card (Master)" : "Unfroze Debit Card (Master)"; 
                logger.info("Toggled master status for card linked to account {}. New status: {}", accountId, card.isActive());
                break;
            case "online":
                previousStatus = card.isOnlineTransactionsEnabled(); 
                card.setOnlineTransactionsEnabled(!previousStatus);
                description = previousStatus ? "Disabled Online Transactions" : "Enabled Online Transactions"; 
                logger.info("Toggled online transactions for card linked to account {}. New status: {}", accountId, card.isOnlineTransactionsEnabled());
                break;
            case "international":
                previousStatus = card.isInternationalTransactionsEnabled(); 
                card.setInternationalTransactionsEnabled(!previousStatus);
                description = previousStatus ? "Disabled International Transactions" : "Enabled International Transactions"; 
                logger.info("Toggled international transactions for card linked to account {}. New status: {}", accountId, card.isInternationalTransactionsEnabled());
                break;
            default:
                logger.warn("Invalid card toggle option received: {}", option);
                throw new IllegalArgumentException("Invalid card option specified: " + option);
        }
        
        // --- ADD LOGGING ---
        String accNumSuffix = account.getAccountNumber() != null && account.getAccountNumber().length() >= 4 ? account.getAccountNumber().substring(account.getAccountNumber().length() - 4) : "N/A";
        ActivityLog logEntry = new ActivityLog(
            account.getUser(),               
            activityType,          
            description + " for Account ending in " + accNumSuffix
        );
        activityLogRepository.save(logEntry); // Save the log entry
        // --- END LOGGING ---

        debitCardRepository.save(card); // Save the updated card
        return new DebitCardDto(card);  // Return the updated card state
    }
    
    // --- Activity Log Fetching ---
    @Transactional(readOnly = true) 
    public List<ActivityLogDto> getActivityLogsForUser(String username) {
        List<ActivityLog> logs = activityLogRepository.findByUserUsernameOrderByTimestampDesc(username);
        return logs.stream()
                   .map(ActivityLogDto::new) 
                   .collect(Collectors.toList());
    }
    // --- THIS IS A CONCEPTUAL METHOD - You would build this later ---
@Transactional
public void submitLoanApplication(String username, LoanApplicationRequest request) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));

    // --- HERE: You would add logic to validate and save the loan application details ---
    // For example:
    // LoanApplication newApp = new LoanApplication(user, request.getAmount(), request.getPurpose(), request.getMonthlyIncome());
    // loanApplicationRepository.save(newApp); 
    // --- End of saving logic ---

    // --- ADD LOGGING BLOCK HERE ---
    try {
        logger.info("Attempting to save loan application activity log for user: {}", username); 
        // Format the amount nicely for the log description
        String formattedAmount = formatCurrencyForLog(request.getAmount()); // You might need a simple helper for this
        ActivityLog logEntry = new ActivityLog(
            user, 
            "LOAN_APPLICATION", 
            "Submitted loan application for " + formattedAmount + " (Purpose: " + request.getPurpose() + ")"
        );
        activityLogRepository.save(logEntry);
        logger.info("Successfully saved loan application activity log for user: {}", username); 
    } catch (Exception e) {
        logger.error("Error saving loan application activity log for user {}: {}", username, e.getMessage(), e);
    }
    // --- END LOGGING BLOCK ---

    // Potentially return something, like the application ID
}

// Helper to format currency for logs (avoids complex locale formatting if not needed)
private String formatCurrencyForLog(BigDecimal amount) {
     if (amount == null) return "N/A";
     // Simple INR formatting for logs
     return "₹" + String.format("%,.2f", amount); 
}
// --- END OF CONCEPTUAL METHOD ---
}
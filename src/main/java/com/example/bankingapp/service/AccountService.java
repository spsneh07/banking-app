package com.example.bankingapp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator; // Import Comparator if combining logs (optional)
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger; // For null-safe comparisons
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
import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.ActivityLog;
import com.example.bankingapp.model.DebitCard;
import com.example.bankingapp.model.Transaction;
import com.example.bankingapp.model.TransactionType;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.AccountRepository;
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
    @Autowired private ActivityLogRepository activityLogRepository;

    // --- Methods that log USER-ONLY activity ---
    // (updateUserProfile, changeUserPassword, setPin remain the same - they correctly use the user-only ActivityLog constructor)
  @Transactional
    public User updateUserProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // --- Check for existing email ---
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (!existingUser.getUsername().equals(username)) {
                throw new IllegalArgumentException("Email is already in use by another account.");
            }
        });

        // --- DETAILED CHANGE DETECTION ---
        List<String> changes = new ArrayList<>(); // List to store descriptions of changes
        
        // Check Full Name
        if (!Objects.equals(user.getFullName(), request.getFullName())) {
            changes.add("Full Name");
        }
        // Check Email
        if (!Objects.equals(user.getEmail(), request.getEmail())) {
            changes.add("Email");
        }
        // Check Phone Number
        if (!Objects.equals(user.getPhoneNumber(), request.getPhoneNumber())) {
            changes.add("Phone Number");
        }
        // Check Address
        if (!Objects.equals(user.getAddress(), request.getAddress())) {
            changes.add("Address");
        }
        // Check Nominee Name
        if (!Objects.equals(user.getNomineeName(), request.getNomineeName())) {
            changes.add("Nominee Name");
        }
        // Check Date of Birth
        if (!Objects.equals(user.getDateOfBirth(), request.getDateOfBirth())) {
            changes.add("Date of Birth");
        }
        // --- End Change Detection ---

        // Only proceed if changes were detected
        if (!changes.isEmpty()) {
            logger.info("Profile changes detected for user {}: {}", username, changes);
            
            // Set all new fields (only save operation needed)
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setAddress(request.getAddress());
            user.setNomineeName(request.getNomineeName());
            
            User savedUser = userRepository.save(user); // Save the updated user

            // --- CREATE DETAILED LOG DESCRIPTION ---
            String logDescription;
            if (changes.size() == 1) {
                logDescription = "Updated " + changes.get(0); // e.g., "Updated Email"
            } else {
                // Join multiple changes, e.g., "Updated Full Name, Address, and Nominee Name"
                logDescription = "Updated " + String.join(", ", changes.subList(0, changes.size() - 1)) 
                                 + ", and " + changes.get(changes.size() - 1); 
            }
            // --- END DESCRIPTION CREATION ---

            // --- LOGGING BLOCK ---
            try {
                logger.info("Attempting to save profile update activity log for user: {}", username); 
                ActivityLog logEntry = new ActivityLog(savedUser, "PROFILE_UPDATE", logDescription); // Use detailed description
                activityLogRepository.save(logEntry);
                logger.info("Successfully saved profile update activity log for user: {}", username); 
            } catch (Exception e) {
                logger.error("Error saving profile update activity log for user {}: {}", username, e.getMessage(), e);
            }
            // --- END LOGGING BLOCK ---
            
            return savedUser; // Return the saved user
        } else {
            logger.info("No profile changes detected for user: {}", username);
            return user; // No changes, return original user object
        }
    }

    // ... (rest of AccountService.java) ...

    // ... (rest of AccountService.java) ...

    @Transactional
    public void changeUserPassword(String username, String currentPassword, String newPassword) {
        verifyUserPassword(username, currentPassword);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Logging (Uses user-only constructor)
        ActivityLog logEntry = new ActivityLog(user, "PASSWORD_CHANGE", "Changed account password");
        activityLogRepository.save(logEntry);
    }

    @Transactional
    public void setPin(String username, String currentPassword, String newPin) {
        verifyUserPassword(username, currentPassword);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        boolean wasPinSet = user.getPin() != null;
        user.setPin(passwordEncoder.encode(newPin));
        userRepository.save(user);

        // Logging (Uses user-only constructor)
        String description = wasPinSet ? "Updated security PIN" : "Set initial security PIN";
        ActivityLog logEntry = new ActivityLog(user, "PIN_CHANGE", description);
        activityLogRepository.save(logEntry);
    }

    // --- Transaction methods (No logging added here as per request) ---
    // (deposit, transfer, payBill remain unchanged)
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
    // Inside AccountService.java

@Transactional
public void selfTransfer(String username, Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String providedPin) {
    // 1. Verify PIN first
    verifyUserPin(username, providedPin);

    // 2. Prevent transferring to the same account
    if (sourceAccountId.equals(destinationAccountId)) {
        throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
    }

    // 3. Find and verify ownership of BOTH accounts
    Account sourceAccount = findAccountByIdAndUsername(sourceAccountId, username); // Throws if not found or not owned
    Account destinationAccount = findAccountByIdAndUsername(destinationAccountId, username); // Throws if not found or not owned

    // 4. Check sufficient funds
    if (sourceAccount.getBalance().compareTo(amount) < 0) {
        throw new IllegalArgumentException("Insufficient funds in the source account.");
    }

    // 5. Perform the transfer
    sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
    destinationAccount.setBalance(destinationAccount.getBalance().add(amount));

    // 6. Save account changes
    accountRepository.save(sourceAccount);
    accountRepository.save(destinationAccount);

    // 7. Record transactions for both accounts
    String sourceDesc = "Self-transfer to Acc ending " + destinationAccount.getAccountNumber().substring(destinationAccount.getAccountNumber().length() - 4);
    String destDesc = "Self-transfer from Acc ending " + sourceAccount.getAccountNumber().substring(sourceAccount.getAccountNumber().length() - 4);

    Transaction sourceTx = new Transaction(TransactionType.TRANSFER, amount.negate(), sourceDesc, sourceAccount);
    Transaction destTx = new Transaction(TransactionType.TRANSFER, amount, destDesc, destinationAccount);

    transactionRepository.save(sourceTx);
    transactionRepository.save(destTx);

    // 8. Log the activity (User-level log)
    User user = sourceAccount.getUser(); // Get user from one of the accounts
    String logDesc = "Transferred " + formatCurrencyForLog(amount) + 
                     " from Acc ending " + sourceAccount.getAccountNumber().substring(sourceAccount.getAccountNumber().length() - 4) +
                     " to Acc ending " + destinationAccount.getAccountNumber().substring(destinationAccount.getAccountNumber().length() - 4);
    ActivityLog logEntry = new ActivityLog(user, "SELF_TRANSFER", logDesc); 
    activityLogRepository.save(logEntry);

    logger.info("Self-transfer successful for user {}: {} from account {} to account {}", username, amount, sourceAccountId, destinationAccountId);
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
    // (getBalance, getRecentTransactions, verifyRecipient, createInitialDepositTransaction, getAccountsForUser remain unchanged)
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
    // (findAccountByIdAndUsername, verifyUserPassword, verifyUserPin remain unchanged)
    private Account findAccountByIdAndUsername(Long accountId, String username) {
        return accountRepository.findById(accountId)
                .filter(acc -> acc.getUser().getUsername().equals(username))
                .orElseThrow(() -> new RuntimeException("Account not found or user does not own this account."));
    }
    
    private void verifyUserPassword(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid password. Transaction authorization failed.");
        }
    }

    public void deactivateUser(Long id) {
        // Find the user by their ID
        User user = userRepository.findById(id).orElse(null);

        // Check if the user was found
        if (user != null) {
            // Change the status
            user.setAccountStatus("INACTIVE");
            
            // Save the updated user object
            userRepository.save(user);
        }
    }
    public User findByUsername(String username) {
    // This now correctly handles the 'Optional'
    return userRepository.findByUsername(username)
           .orElse(null); // If not found, return null
}

    /**
     * Saves a new user during registration.
     */
    public void saveUser(User user) {
        // We will add password encoding here later!
        userRepository.save(user);
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
     private String formatCurrencyForLog(BigDecimal amount) {
         if (amount == null) return "N/A";
         return "₹" + String.format("%,.2f", amount); 
    }

    // --- Card related methods ---
    // (getDebitCardDetails, getDebitCardCvv remain unchanged)
     @Transactional(readOnly = true)
    public DebitCardDto getDebitCardDetails(Long accountId, String username) {
        Account account = findAccountByIdAndUsername(accountId, username);
        if (account.getDebitCard() == null) {
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

    // --- CORRECTED toggleDebitCardOption ---
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

        // (Switch statement remains the same)
        switch (option.toLowerCase()) {
            case "master":
                previousStatus = card.isActive(); 
                card.setActive(!previousStatus);
                description = previousStatus ? "Froze Debit Card (Master)" : "Unfroze Debit Card (Master)"; 
                logger.info("Toggled master status...");
                break;
            case "online":
                previousStatus = card.isOnlineTransactionsEnabled(); 
                card.setOnlineTransactionsEnabled(!previousStatus);
                description = previousStatus ? "Disabled Online Transactions" : "Enabled Online Transactions"; 
                logger.info("Toggled online transactions...");
               break;
            case "international":
                previousStatus = card.isInternationalTransactionsEnabled(); 
                card.setInternationalTransactionsEnabled(!previousStatus);
                description = previousStatus ? "Disabled International Transactions" : "Enabled International Transactions"; 
                logger.info("Toggled international transactions...");
                break;
            default:
                logger.warn("Invalid card toggle option received: {}", option);
                throw new IllegalArgumentException("Invalid card option specified: " + option);
        }
        
        // --- CORRECTED LOGGING (Uses constructor with Account) ---
        String accNumSuffix = account.getAccountNumber() != null && account.getAccountNumber().length() >= 4 ? account.getAccountNumber().substring(account.getAccountNumber().length() - 4) : "N/A";
        // Use the constructor WITH the 'account' object
        ActivityLog logEntry = new ActivityLog( 
            account.getUser(),               
            account, // <<< PASS THE ACCOUNT OBJECT HERE
            activityType,          
            description + " for Account ending in " + accNumSuffix
        );
        activityLogRepository.save(logEntry); 
        // --- END CORRECTION ---

        debitCardRepository.save(card); 
        return new DebitCardDto(card);  
    }
    // --- END CORRECTION ---
    @Transactional
    public void verifyPasswordAndDeactivate(String username, String password) {
        // 1. Verify the password first
        // This uses your existing private helper method
        verifyUserPassword(username, password); 
        
        // 2. If password is valid, find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found after password verification."));
        
        // 3. Deactivate the user (this is your existing logic)
        user.setAccountStatus("INACTIVE");
        userRepository.save(user);

        // 4. Log this major event
        ActivityLog logEntry = new ActivityLog(user, "ACCOUNT_DEACTIVATED", "User deactivated their account.");
        activityLogRepository.save(logEntry);
    }
    // --- Loan Application Method (Conceptual, but logging corrected) ---
     @Transactional
     public void submitLoanApplication(String username, Long accountId, LoanApplicationRequest request) { // Assume accountId is passed
         User user = userRepository.findByUsername(username)
                 .orElseThrow(() -> new RuntimeException("User not found: " + username));
         Account account = findAccountByIdAndUsername(accountId, username); // Get account & verify ownership

         // --- HERE: Add logic to save the loan application details ---
         logger.info("Processing loan application for user {} on account {}", username, accountId);
         // Example: 
         // LoanApplication newApp = new LoanApplication(user, account, request.getAmount(), request.getPurpose(), request.getMonthlyIncome());
         // loanApplicationRepository.save(newApp); 
         // --- End of saving logic ---

         // --- CORRECTED LOGGING (Passes the 'account' object) ---
         try {
             logger.info("Attempting to save loan application activity log for user: {}", username); 
             String formattedAmount = formatCurrencyForLog(request.getAmount()); 
             String accNumSuffix = account.getAccountNumber() != null && account.getAccountNumber().length() >= 4 ? account.getAccountNumber().substring(account.getAccountNumber().length() - 4) : "N/A";
             // Use the constructor WITH the 'account' object
             ActivityLog logEntry = new ActivityLog(
                 user, 
                 account, // Pass the relevant account
                 "LOAN_APPLICATION", 
                 "Submitted loan application for " + formattedAmount + " (Purpose: " + request.getPurpose() + ")" + " on Account ending in " + accNumSuffix
             );
             activityLogRepository.save(logEntry);
             logger.info("Successfully saved loan application activity log for user: {}", username); 
         } catch (Exception e) {
             logger.error("Error saving loan application activity log for user {}: {}", username, e.getMessage(), e);
         }
         // --- END LOGGING ---
     }
    
    // --- CORRECTED Activity Log Fetching Method ---
// Inside AccountService.java

    @Transactional(readOnly = true) 
    public List<ActivityLogDto> getActivityLogsForAccount(Long accountId, String username) {
        Account account = findAccountByIdAndUsername(accountId, username); 
        
        // 1. Fetch account-specific logs
        List<ActivityLog> accountLogs = activityLogRepository.findByAccountIdOrderByTimestampDesc(accountId);
        logger.info("Fetched {} logs specific to accountId: {}", accountLogs.size(), accountId); // <-- ADD LOG

        // 2. Fetch user-only logs
        List<ActivityLog> userOnlyLogs = activityLogRepository.findByUserUsernameAndAccountIsNullOrderByTimestampDesc(username);
        logger.info("Fetched {} user-only logs for username: {}", userOnlyLogs.size(), username); // <-- ADD LOG
        
        // 3. Combine
        List<ActivityLog> combinedLogs = new ArrayList<>(accountLogs);
        combinedLogs.addAll(userOnlyLogs);
        logger.info("Total combined logs before sorting: {}", combinedLogs.size()); // <-- ADD LOG

        // 4. Sort
        combinedLogs.sort(Comparator.comparing(ActivityLog::getTimestamp).reversed());

        // 5. Convert to DTOs
        return combinedLogs.stream()
                           .map(ActivityLogDto::new) 
                           .collect(Collectors.toList());
    }
}
package com.example.bankingapp.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException; // <-- Import Map
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingapp.dto.TransactionDto;
import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.Transaction;
import com.example.bankingapp.model.TransactionType;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.AccountRepository;
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

    // --- NEW: GET SPENDING SUMMARY FOR CHART ---
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSpendingSummary(String username) {
        Account account = findAccountByUsername(username);
        
        // Fetch ALL transactions for this account
        List<Transaction> allTransactions = transactionRepository.findAllByAccountId(account.getId());

        // Filter for expenses (negative amounts) and group by type
        Map<String, BigDecimal> spendingSummary = allTransactions.stream()
            .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0) // Only expenses
            .collect(Collectors.groupingBy(
                tx -> tx.getType().toString(), // Group by "TRANSFER", "PAYMENT"
                Collectors.mapping(
                    Transaction::getAmount, // Get the amount
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add) // Sum them up
                )
            ));

        // Make the summed values positive for the chart
        spendingSummary.replaceAll((type, total) -> total.abs());
        
        return spendingSummary;
    }
    // -----------------------------------------

    // --- All other methods remain the same ---
    
    @Transactional
    public User updateUserProfile(String username, String newFullName, String newEmail) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
            if (!existingUser.getUsername().equals(username)) {
                throw new IllegalArgumentException("Email is already in use by another account.");
            }
        });
        user.setFullName(newFullName);
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    @Transactional
    public void changeUserPassword(String username, String currentPassword, String newPassword) {
        verifyUserPassword(username, currentPassword);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deposit(String username, BigDecimal amount) {
        Account account = findAccountByUsername(username);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        Transaction transaction = new Transaction(TransactionType.DEPOSIT, amount, "Deposit to account", account);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void transfer(String senderUsername, String recipientAccountNumber, BigDecimal amount, String providedPassword) {
        verifyUserPassword(senderUsername, providedPassword);
        Account senderAccount = findAccountByUsername(senderUsername);
        Account recipientAccount = accountRepository.findByAccountNumber(recipientAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Recipient account number not found."));
        if (senderAccount.getId().equals(recipientAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer money to your own account.");
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
    public void payBill(String username, String billerName, BigDecimal amount, String providedPassword) {
        verifyUserPassword(username, providedPassword);
        Account account = findAccountByUsername(username);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for bill payment.");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Transaction transaction = new Transaction(TransactionType.PAYMENT, amount.negate(), "Paid bill to " + billerName, account);
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String username) {
         return findAccountByUsername(username).getBalance();
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDto> getRecentTransactions(String username) {
        Account account = findAccountByUsername(username);
        return transactionRepository.findTop10ByAccountIdOrderByTimestampDesc(account.getId())
            .stream().map(TransactionDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String verifyRecipient(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Recipient account number not found."));
        String fullName = account.getUser().getFullName();
        if (fullName == null || fullName.trim().isEmpty()) return "N/A";
        String[] names = fullName.split("\\s+");
        if (names.length > 1) return names[0] + " " + names[names.length - 1].charAt(0) + ".";
        return fullName;
    }

    private Account findAccountByUsername(String username) {
        return accountRepository.findByUserUsername(username)
            .orElseThrow(() -> new RuntimeException("Account not found for user: " + username));
    }
    
    private void verifyUserPassword(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid password. Transaction authorization failed.");
        }
    }
}
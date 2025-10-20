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

    @Transactional
    public User updateUserProfile(String username, String newFullName, String newEmail) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
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
        User user = userRepository.findByUsername(username)
             .orElseThrow(() -> new RuntimeException("User not found: " + username));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Transactional
    public void setPin(String username, String password, String newPin) {
        verifyUserPassword(username, password);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        user.setPin(passwordEncoder.encode(newPin));
        userRepository.save(user);
    }

    // --- Transaction methods are now account-specific ---
    @Transactional
    public void deposit(String username, Long accountId, BigDecimal amount) {
        // We find by username AND accountId for security, though verifyAccountOwner in controller does this
        Account account = findAccountByIdAndUsername(accountId, username);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        Transaction transaction = new Transaction(TransactionType.DEPOSIT, amount, "Deposit to account", account);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void transfer(String senderUsername, Long senderAccountId, String recipientAccountNumber, BigDecimal amount, String providedPassword) {
        verifyUserPassword(senderUsername, providedPassword);
        
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
    public void payBill(String username, Long accountId, String billerName, BigDecimal amount, String providedPassword) {
        verifyUserPassword(username, providedPassword);
        
        Account account = findAccountByIdAndUsername(accountId, username);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for bill payment.");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Transaction transaction = new Transaction(TransactionType.PAYMENT, amount.negate(), "Paid bill to " + billerName, account);
        transactionRepository.save(transaction);
    }

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
        // Convert the list of Account entities to a list of AccountDto objects
        return accounts.stream()
                       .map(AccountDto::new)
                       .collect(Collectors.toList());
    }
    
    // Helper to find a user's *specific* account
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
}
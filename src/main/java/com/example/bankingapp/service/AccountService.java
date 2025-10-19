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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingapp.dto.TransactionDto;
import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.Transaction;
import com.example.bankingapp.model.TransactionType;
import com.example.bankingapp.repository.AccountRepository;
import com.example.bankingapp.repository.TransactionRepository;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private AuthenticationManager authenticationManager;

    @Transactional
    public void deposit(String username, BigDecimal amount) {
        Account account = findAccountByUsername(username);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        Transaction transaction = new Transaction(TransactionType.DEPOSIT, amount, "Deposit to account", account);
        transactionRepository.save(transaction);
        logger.info("Deposit successful for user {}", username);
    }

    @Transactional
    public void transfer(String senderUsername, String recipientAccountNumber, BigDecimal amount, String providedPassword) {
        verifyUserPassword(senderUsername, providedPassword);

        Account senderAccount = findAccountByUsername(senderUsername);
        
        // --- FIND RECIPIENT BY ACCOUNT NUMBER ---
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
        
        logger.info("Transfer successful from {} to {}", senderUsername, recipientAccount.getUser().getUsername());
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
        logger.info("Bill payment successful for user {}", username);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String username) {
         return findAccountByUsername(username).getBalance();
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDto> getRecentTransactions(String username) {
        Account account = findAccountByUsername(username);
        return transactionRepository.findTop10ByAccountIdOrderByTimestampDesc(account.getId())
            .stream()
            .map(TransactionDto::new)
            .collect(Collectors.toList());
    }

    // --- UPDATED VERIFICATION METHOD ---
    @Transactional(readOnly = true)
    public String verifyRecipient(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Recipient account number not found."));
        return account.getUser().getFullName();
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

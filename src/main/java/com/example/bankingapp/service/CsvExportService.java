package com.example.bankingapp.service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.Transaction;
import com.example.bankingapp.repository.AccountRepository;
import com.example.bankingapp.repository.TransactionRepository;

@Service
public class CsvExportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvExportService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository; 

    @Transactional(readOnly = true)
    public void writeTransactionsToCsv(Writer writer, String username, Long accountId) {
        
        // 1. SECURITY CHECK
        Account account = accountRepository.findById(accountId)
            .filter(acc -> acc.getUser().getUsername().equals(username))
            .orElseThrow(() -> new RuntimeException("Account not found or user does not own this account."));
        
        // 2. Fetch all transactions
        List<Transaction> transactions;
        try {
            // Using your method:
            transactions = transactionRepository.findAllByAccountId(accountId);
            // (For better sorting, rename in repo: findAllByAccountIdOrderByTimestampDesc)
            
        } catch (Exception e) {
            logger.error("Error fetching transactions for CSV export for account: {}", accountId, e);
            throw new RuntimeException("Could not fetch transaction data for CSV export.", e);
        }

        // 3. Define CSV Headers
        // --- MODIFIED --- (Removed "Balance After")
        String[] headers = {"Transaction ID", "Date", "Description", "Type", "Amount"};

        // 4. Use Apache Commons CSV to write data
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))) {
            
            for (Transaction tx : transactions) {
                csvPrinter.printRecord(
                    tx.getId(),
                    tx.getTimestamp(),
                    tx.getDescription(),
                    tx.getType().toString(), 
                    tx.getAmount()
                    // --- MODIFIED --- (Removed tx.getBalanceAfterTransaction())
                );
            }
            logger.info("Successfully generated CSV for user: {}, account: {}", username, accountId);
        } catch (IOException e) {
            logger.error("Error while writing CSV for user: {}, account: {}", username, accountId, e);
            throw new RuntimeException("Failed to generate CSV file", e);
        }
    }
}
package com.example.bankingapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find the top 10 most recent transactions for a given account, ordered by time
    List<Transaction> findTop10ByAccountIdOrderByTimestampDesc(Long accountId);
}
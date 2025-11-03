package com.example.bankingapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findTop10ByAccountIdOrderByTimestampDesc(Long accountId);
    List<Transaction> findAllByAccountIdOrderByTimestampDesc(Long accountId);
    // --- ADD THIS NEW METHOD ---
    List<Transaction> findAllByAccountId(Long accountId);
    // -------------------------
}
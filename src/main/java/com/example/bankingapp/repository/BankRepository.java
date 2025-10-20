package com.example.bankingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.Bank;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
}
package com.example.bankingapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.DebitCard;

@Repository
public interface DebitCardRepository extends JpaRepository<DebitCard, Long> {
    
    // We'll need this to find a card by its account
    Optional<DebitCard> findByAccountId(Long accountId);
}
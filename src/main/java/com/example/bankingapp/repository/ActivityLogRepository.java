package com.example.bankingapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankingapp.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Finds logs for a specific user, ordered by timestamp
    List<ActivityLog> findByUserUsernameOrderByTimestampDesc(String username); 

    // --- ENSURE THIS LINE EXISTS EXACTLY AS WRITTEN ---
    List<ActivityLog> findByAccountIdOrderByTimestampDesc(Long accountId);
    // --- END ---
    List<ActivityLog> findByUserUsernameAndAccountIsNullOrderByTimestampDesc(String username);
}
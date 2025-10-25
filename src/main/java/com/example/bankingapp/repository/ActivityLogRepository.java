// src/main/java/com/example/bankingapp/repository/ActivityLogRepository.java
package com.example.bankingapp.repository;

import com.example.bankingapp.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    // Finds all logs for a given username, ordered by timestamp descending
    List<ActivityLog> findByUserUsernameOrderByTimestampDesc(String username); 
}
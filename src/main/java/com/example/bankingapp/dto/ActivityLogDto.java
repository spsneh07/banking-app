// src/main/java/com/example/bankingapp/dto/ActivityLogDto.java
package com.example.bankingapp.dto;

import com.example.bankingapp.model.ActivityLog;
import lombok.Data;
import java.time.LocalDateTime;

@Data // Includes getters, setters, toString, etc.
public class ActivityLogDto {

    private Long id;
    private LocalDateTime timestamp;
    private String activityType;
    private String description;

    // Constructor to convert Entity to DTO
    public ActivityLogDto(ActivityLog log) {
        this.id = log.getId();
        this.timestamp = log.getTimestamp();
        this.activityType = log.getActivityType();
        this.description = log.getDescription();
    }
}
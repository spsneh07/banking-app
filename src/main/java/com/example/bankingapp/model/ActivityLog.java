package com.example.bankingapp.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column; // Make sure all jakarta.persistence annotations are imported
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // Specifies that this class is a JPA entity
@Table(name = "activity_logs") // Maps this entity to the "activity_logs" table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok annotation for a no-argument constructor (required by JPA)
public class ActivityLog {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configures auto-increment for the ID
    private Long id;

    // Establishes a many-to-one relationship with the User entity
    // Many activity logs can belong to one user
    @ManyToOne(fetch = FetchType.LAZY) // LAZY fetching means the User object isn't loaded until explicitly requested
    @JoinColumn(name = "user_id", nullable = false) // Specifies the foreign key column ("user_id") linking to the User's primary key
    private User user;

    // --- MISSING RELATIONSHIP ---
    // You need to add the @ManyToOne relationship to Account here
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "account_id", nullable = true) // Nullable because some logs are user-only
    private Account account;
    // --- END MISSING RELATIONSHIP ---

    @Column(nullable = false) // Ensures the timestamp cannot be null in the database
    private LocalDateTime timestamp; // Records when the activity occurred

    @Column(nullable = false) // Ensures the activity type cannot be null
    private String activityType; // A category for the activity (e.g., "PROFILE_UPDATE", "PIN_CHANGE")

    @Column(nullable = false, length = 255) // Ensures description isn't null and sets a max length
    private String description; // A user-friendly description of the activity

    // --- MISSING CONSTRUCTOR ---
    // You need the constructor that accepts the Account object
    public ActivityLog(User user, Account account, String activityType, String description) {
        this.user = user;
        this.account = account; // Links to the specific account
        this.activityType = activityType;
        this.description = description;
        this.timestamp = LocalDateTime.now(); // Sets timestamp automatically
    }
    // --- END MISSING CONSTRUCTOR ---

    // Constructor for user-only logs (This one is correct)
    public ActivityLog(User user, String activityType, String description) {
         this(user, null, activityType, description); // Calls the main constructor with null account
    }
}
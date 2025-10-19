package com.example.bankingapp.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // This is the "magic" method you wrote about in your report.
    // Spring Data JPA automatically creates the query from the method name.
    // We will use this for logging in.
    Optional<User> findByUsername(String username);

    // We'll use this to check for duplicates (TC-02)
    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
package com.example.bankingapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankingapp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // --- ADD THIS METHOD ---
    // This allows the service to check if an email is already in use.
    Optional<User> findByEmail(String email);
    // -----------------------

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}

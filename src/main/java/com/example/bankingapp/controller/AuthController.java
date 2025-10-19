package com.example.bankingapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingapp.dto.JwtResponse;
import com.example.bankingapp.dto.LoginRequest;
import com.example.bankingapp.dto.RegisterRequest;
import com.example.bankingapp.model.Account;
import com.example.bankingapp.model.User;
import com.example.bankingapp.repository.AccountRepository;
import com.example.bankingapp.repository.UserRepository;
import com.example.bankingapp.security.JwtTokenProvider;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private AccountRepository accountRepository;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        logger.info("Login attempt for user: {}", loginRequest.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            logger.info("Authentication successful for user: {}", loginRequest.getUsername());
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}. Reason: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
             logger.error("An unexpected error occurred during login for user: {}", loginRequest.getUsername(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        logger.info("JWT generated successfully for user: {}", loginRequest.getUsername());

        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
             logger.warn("Registration failed: Username {} already taken", registerRequest.getUsername());
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
             logger.warn("Registration failed: Email {} already in use", registerRequest.getEmail());
            return new ResponseEntity<>("Email is already in use!", HttpStatus.BAD_REQUEST);
        }

        // 1. Create the user with their full name
        User user = new User(
            registerRequest.getFullName(), // <<< THIS IS THE FIX
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            passwordEncoder.encode(registerRequest.getPassword())
        );
        userRepository.save(user);

        // 2. Create an account for the new user
        Account newAccount = new Account();
        newAccount.setUser(user);
        accountRepository.save(newAccount);
        logger.info("Created new account for user: {}", user.getUsername());

        logger.info("User registered successfully: {}", registerRequest.getUsername());

        return new ResponseEntity<>("User registered successfully", HttpStatus.OK);
    }
}

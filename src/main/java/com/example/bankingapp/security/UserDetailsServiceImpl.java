package com.example.bankingapp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service; // Correct interface
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingapp.model.User; // Correct annotation
import com.example.bankingapp.repository.UserRepository;

@Service // This class MUST have @Service
public class UserDetailsServiceImpl implements UserDetailsService { // Correct interface

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Attempting to load user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User Not Found with username: " + username);
                });

        logger.info("User found: {}", username);
        // Calls the static build method from the *other* class
        return UserDetailsImpl.build(user);
    }
}
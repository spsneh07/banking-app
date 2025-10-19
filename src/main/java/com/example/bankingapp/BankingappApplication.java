package com.example.bankingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
// -------------------------------------


// --- ADD THESE 3 ANNOTATIONS ---
@ComponentScan("com.example.bankingapp")
@EntityScan("com.example.bankingapp.model")
@EnableJpaRepositories("com.example.bankingapp.repository")
// -------------------------------

@SpringBootApplication
public class BankingappApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingappApplication.class, args);
    }
}
package com.solvix.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserDataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String customerUsername;
    private final String customerPassword;
    private final String supportUsername;
    private final String supportPassword;

    public UserDataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${solvix.security.bootstrap.customer.username}") String customerUsername,
            @Value("${solvix.security.bootstrap.customer.password}") String customerPassword,
            @Value("${solvix.security.bootstrap.support.username}") String supportUsername,
            @Value("${solvix.security.bootstrap.support.password}") String supportPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerUsername = customerUsername;
        this.customerPassword = customerPassword;
        this.supportUsername = supportUsername;
        this.supportPassword = supportPassword;
    }

    @Override
    public void run(String... args) {
        createIfMissing(customerUsername, customerPassword, UserRole.CUSTOMER);
        createIfMissing(supportUsername, supportPassword, UserRole.SUPPORT_AGENT);
    }

    private void createIfMissing(String username, String password, UserRole role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.save(new UserEntity(username, passwordEncoder.encode(password), role));
        }
    }
}

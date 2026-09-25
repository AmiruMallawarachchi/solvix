package com.solvix.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/tickets/**").authenticated()
                        .anyRequest().denyAll()
                )
                .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${solvix.security.users.customer.username}") String customerUsername,
            @Value("${solvix.security.users.customer.password}") String customerPassword,
            @Value("${solvix.security.users.support.username}") String supportUsername,
            @Value("${solvix.security.users.support.password}") String supportPassword
    ) {
        return new InMemoryUserDetailsManager(
                User.withUsername(customerUsername)
                        .password(passwordEncoder.encode(customerPassword))
                        .roles("CUSTOMER")
                        .build(),
                User.withUsername(supportUsername)
                        .password(passwordEncoder.encode(supportPassword))
                        .roles("SUPPORT_AGENT")
                        .build()
        );
    }
}

package com.solvix.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Auth0AudienceValidatorTest {
    private final Auth0AudienceValidator validator = new Auth0AudienceValidator("https://api.solvix.example");

    @Test
    void acceptsTokenForConfiguredApiAudience() {
        assertThat(validator.validate(token(List.of("https://api.solvix.example"))).hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenForAnotherApi() {
        OAuth2TokenValidatorResult result = validator.validate(token(List.of("https://another-api.example")));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt token(List<String> audiences) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .audience(audiences)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .build();
    }
}

package com.solvix.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoAccessTokenValidatorTest {
    private final CognitoAccessTokenValidator validator = new CognitoAccessTokenValidator("solvix-client");

    @Test
    void acceptsAccessTokenIssuedForConfiguredClient() {
        OAuth2TokenValidatorResult result = validator.validate(token("access", "solvix-client"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsIdToken() {
        OAuth2TokenValidatorResult result = validator.validate(token("id", "solvix-client"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenIssuedForDifferentClient() {
        OAuth2TokenValidatorResult result = validator.validate(token("access", "another-client"));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt token(String tokenUse, String clientId) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("token_use", tokenUse)
                .claim("client_id", clientId)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .build();
    }
}

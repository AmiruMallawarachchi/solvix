package com.solvix.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Auth0JwtAuthenticationConverterTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Auth0JwtAuthenticationConverter converter = new Auth0JwtAuthenticationConverter(userRepository);

    @Test
    void usesProvisionedDatabaseUsernameAndRoleInsteadOfTokenRoleClaims() {
        UserEntity user = new UserEntity("support-agent", null, UserRole.SUPPORT_AGENT, "auth0|user-123");
        when(userRepository.findByAuth0Subject("auth0|user-123")).thenReturn(Optional.of(user));
        Jwt jwt = token("auth0|user-123").claim("roles", "ADMINISTRATOR").build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("support-agent");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SUPPORT_AGENT");
    }

    @Test
    void rejectsUnknownSubject() {
        when(userRepository.findByAuth0Subject("auth0|unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> converter.convert(token("auth0|unknown").build()))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsDisabledUser() {
        UserEntity user = new UserEntity("disabled", null, UserRole.CUSTOMER, "auth0|disabled", false);
        when(userRepository.findByAuth0Subject("auth0|disabled")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> converter.convert(token("auth0|disabled").build()))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsMissingSubjectWithoutLookingUpNullIdentity() {
        assertThatThrownBy(() -> converter.convert(token(null).build()))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, org.mockito.Mockito.never()).findByAuth0Subject(null);
    }

    private Jwt.Builder token(String subject) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60));
    }
}

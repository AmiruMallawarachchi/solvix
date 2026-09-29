package com.solvix.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoJwtAuthenticationConverterTest {
    private final ProductionSecurityConfig configuration = new ProductionSecurityConfig();

    @Test
    void mapsKnownCognitoGroupsAndScopesToAuthorities() {
        Jwt jwt = token(List.of("SUPPORT_AGENT", "unknown-group", "customer"), "tickets/read tickets/write");

        AbstractAuthenticationToken authentication = configuration
                .cognitoJwtAuthenticationConverter()
                .convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authorityNames(authentication.getAuthorities())).containsExactlyInAnyOrder(
                "ROLE_SUPPORT_AGENT",
                "ROLE_CUSTOMER",
                "SCOPE_tickets/read",
                "SCOPE_tickets/write"
        );
    }

    @Test
    void doesNotGrantRoleForUnknownCognitoGroups() {
        Jwt jwt = token(List.of("owner", "administrator "), null);

        AbstractAuthenticationToken authentication = configuration
                .cognitoJwtAuthenticationConverter()
                .convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authorityNames(authentication.getAuthorities())).isEmpty();
    }

    private Jwt token(List<String> groups, String scope) {
        Instant issuedAt = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("cognito:groups", groups)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60));
        if (scope != null) builder.claim("scope", scope);
        return builder.build();
    }

    private Set<String> authorityNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}

package com.solvix.backend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

final class Auth0JwtAuthenticationConverter implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {
    private static final OAuth2Error UNPROVISIONED_USER = new OAuth2Error(
            "invalid_token",
            "The authenticated account is not enabled for Solvix.",
            null
    );

    private final UserRepository userRepository;

    Auth0JwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new OAuth2AuthenticationException(UNPROVISIONED_USER);
        }

        UserEntity user = userRepository.findByAuth0Subject(subject)
                .filter(UserEntity::isEnabled)
                .orElseThrow(() -> new OAuth2AuthenticationException(UNPROVISIONED_USER));

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.getUsername()
        );
    }
}

package com.solvix.backend.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token",
            "The JWT must be an access token issued to this application.",
            null
    );

    private final String clientId;

    CognitoAccessTokenValidator(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (!"access".equals(token.getClaimAsString("token_use"))
                || !clientId.equals(token.getClaimAsString("client_id"))) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }
}

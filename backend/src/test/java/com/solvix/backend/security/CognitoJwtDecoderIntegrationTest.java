package com.solvix.backend.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitoJwtDecoderIntegrationTest {
    private static final String ISSUER = "https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_test";
    private static final String CLIENT_ID = "solvix-client";

    private HttpServer jwksServer;
    private KeyPair keyPair;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyID("solvix-test-key")
                .build();
        byte[] jwks = new JWKSet(publicJwk).toString().getBytes(StandardCharsets.UTF_8);
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        jwksServer.start();

        String jwksUri = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks";
        decoder = new ProductionSecurityConfig().cognitoJwtDecoder(ISSUER, jwksUri, CLIENT_ID);
    }

    @AfterEach
    void tearDown() {
        if (jwksServer != null) jwksServer.stop(0);
    }

    @Test
    void acceptsCorrectlySignedCognitoAccessToken() throws Exception {
        Jwt decoded = decoder.decode(token(ISSUER, CLIENT_ID, "access", Instant.now().plusSeconds(60)));

        assertThat(decoded.getSubject()).isEqualTo("user-123");
        assertThat(decoded.getClaimAsString("token_use")).isEqualTo("access");
    }

    @Test
    void rejectsIncorrectIssuer() throws Exception {
        assertInvalidToken(token("https://attacker.example/issuer", CLIENT_ID, "access", Instant.now().plusSeconds(60)));
    }

    @Test
    void rejectsTokenForAnotherClient() throws Exception {
        assertInvalidToken(token(ISSUER, "another-client", "access", Instant.now().plusSeconds(60)));
    }

    @Test
    void rejectsIdToken() throws Exception {
        assertInvalidToken(token(ISSUER, CLIENT_ID, "id", Instant.now().plusSeconds(60)));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        assertInvalidToken(token(ISSUER, CLIENT_ID, "access", Instant.now().minusSeconds(60)));
    }

    private void assertInvalidToken(String encodedToken) {
        assertThatThrownBy(() -> decoder.decode(encodedToken)).isInstanceOf(JwtException.class);
    }

    private String token(String issuer, String clientId, String tokenUse, Instant expiresAt) throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(1);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("user-123")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("client_id", clientId)
                .claim("token_use", tokenUse)
                .claim("scope", "openid")
                .claim("cognito:groups", List.of("CUSTOMER"))
                .build();
        SignedJWT signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("solvix-test-key").build(),
                claims
        );
        signed.sign(new RSASSASigner(keyPair.getPrivate()));
        return signed.serialize();
    }
}

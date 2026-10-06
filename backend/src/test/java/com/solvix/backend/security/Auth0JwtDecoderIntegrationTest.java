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
import java.util.UUID;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Auth0JwtDecoderIntegrationTest {
    private static final String ISSUER = "https://solvix-test.us.auth0.com/";
    private static final String AUDIENCE = "https://api.solvix.example";

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
        decoder = new ProductionSecurityConfig().auth0JwtDecoder(ISSUER, jwksUri, AUDIENCE);
    }

    @AfterEach
    void tearDown() {
        if (jwksServer != null) jwksServer.stop(0);
    }

    @Test
    void acceptsCorrectlySignedTokenForConfiguredApi() throws Exception {
        Jwt decoded = decoder.decode(token(ISSUER, AUDIENCE, Instant.now().plusSeconds(60)));

        assertThat(decoded.getSubject()).isEqualTo("auth0|user-123");
        assertThat(decoded.getAudience()).contains(AUDIENCE);
    }

    @Test
    void rejectsIncorrectIssuer() throws Exception {
        assertInvalidToken(token("https://attacker.example/issuer", AUDIENCE, Instant.now().plusSeconds(60)));
    }

    @Test
    void rejectsTokenForAnotherApi() throws Exception {
        assertInvalidToken(token(ISSUER, "https://another-api.example", Instant.now().plusSeconds(60)));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        assertInvalidToken(token(ISSUER, AUDIENCE, Instant.now().minusSeconds(60)));
    }

    private void assertInvalidToken(String encodedToken) {
        assertThatThrownBy(() -> decoder.decode(encodedToken)).isInstanceOf(JwtException.class);
    }

    private String token(String issuer, String audience, Instant expiresAt) throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(1);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("auth0|user-123")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .audience(audience)
                .build();
        SignedJWT signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("solvix-test-key").build(),
                claims
        );
        signed.sign(new RSASSASigner(keyPair.getPrivate()));
        return signed.serialize();
    }
}

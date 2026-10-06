package com.solvix.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "solvix.security.auth0.issuer-uri=https://solvix-test.us.auth0.com/",
        "solvix.security.auth0.jwk-set-uri=https://solvix-test.us.auth0.com/.well-known/jwks.json",
        "solvix.security.auth0.audience=https://api.solvix.example",
        "solvix.security.cors.allowed-origin=http://localhost:3000"
})
@AutoConfigureMockMvc
@ActiveProfiles("production")
class ProductionSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void productionProfileRequiresAuth0AuthenticationAndDoesNotBootstrapLocalUsers() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer-1\",\"password\":\"change-me\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(applicationContext.containsBean("userDataInitializer")).isFalse();
    }

    @Test
    void productionCorsAllowsConfiguredFrontendPreflight() throws Exception {
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/v1/tickets");
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(preflight);

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(corsConfiguration.checkHttpMethod(HttpMethod.POST)).isNotNull();
        assertThat(corsConfiguration.checkHeaders(List.of("authorization", "content-type"))).isNotNull();
        assertThat(corsConfiguration.checkOrigin("https://untrusted.example")).isNull();
        assertThat(corsConfiguration.checkHttpMethod(HttpMethod.DELETE)).isNull();
        assertThat(corsConfiguration.checkHeaders(List.of("x-unapproved-header"))).isNull();

        mockMvc.perform(options("/api/v1/tickets")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }
}

package com.solvix.backend.web;

import com.solvix.backend.security.UserAdministrationAuditRepository;
import com.solvix.backend.security.UserEntity;
import com.solvix.backend.security.UserRepository;
import com.solvix.backend.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "solvix.security.auth0.issuer-uri=https://solvix-test.us.auth0.com/",
        "solvix.security.auth0.jwk-set-uri=https://solvix-test.us.auth0.com/.well-known/jwks.json",
        "solvix.security.auth0.audience=https://api.solvix.example",
        "solvix.security.cors.allowed-origin=http://localhost:3000"
})
@AutoConfigureMockMvc
@ActiveProfiles("production")
class UserAdministrationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAdministrationAuditRepository auditRepository;

    @BeforeEach
    void prepareAdministrator() {
        auditRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new UserEntity(
                "pilot-admin",
                null,
                UserRole.ADMINISTRATOR,
                "auth0|pilot-admin"
        ));
    }

    @Test
    @WithMockUser(username = "pilot-admin", roles = "ADMINISTRATOR")
    void administratorCanProvisionAndDisableAnAuth0MappedAccount() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-agent",
                                  "auth0Subject": "auth0|new-agent",
                                  "role": "SUPPORT_AGENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new-agent"))
                .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"))
                .andExpect(jsonPath("$.enabled").value(true));

        UserEntity provisioned = userRepository.findByAuth0Subject("auth0|new-agent").orElseThrow();
        assertThat(provisioned.getPasswordHash()).isNull();
        assertThat(auditRepository.findAll()).hasSize(1);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(not(containsString("auth0|"))));

        mockMvc.perform(patch("/api/v1/admin/users/{username}/enabled", "new-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(userRepository.findByAuth0Subject("auth0|new-agent").orElseThrow().isEnabled()).isFalse();
        assertThat(auditRepository.findAll()).hasSize(2);
    }

    @Test
    @WithMockUser(username = "pilot-admin", roles = "ADMINISTRATOR")
    void administratorCannotProvisionAnAuth0SubjectTwice() throws Exception {
        userRepository.save(new UserEntity("existing-agent", null, UserRole.SUPPORT_AGENT, "auth0|existing-agent"));

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "another-agent",
                                  "auth0Subject": "auth0|existing-agent",
                                  "role": "SUPPORT_AGENT"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "pilot-admin", roles = "ADMINISTRATOR")
    void administratorCannotDisableTheLastEnabledAdministrator() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{username}/enabled", "pilot-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict());

        assertThat(userRepository.findByUsername("pilot-admin").orElseThrow().isEnabled()).isTrue();
        assertThat(auditRepository.findAll()).isEmpty();
    }

    @Test
    @WithMockUser(username = "pilot-customer", roles = "CUSTOMER")
    void customerCannotAccessUserAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestCannotAccessUserAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}

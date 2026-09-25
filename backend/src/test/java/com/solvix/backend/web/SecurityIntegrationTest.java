package com.solvix.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvix.backend.infrastructure.ticket.SpringDataTicketRepository;
import com.solvix.backend.security.UserEntity;
import com.solvix.backend.security.UserRepository;
import com.solvix.backend.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    private static final String CUSTOMER_PASSWORD = "test-customer-password";
    private static final String SUPPORT_PASSWORD = "test-support-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataTicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanTickets() {
        ticketRepository.deleteAll();
        if (userRepository.findByUsername("test-customer-2").isEmpty()) {
            userRepository.save(new UserEntity(
                    "test-customer-2",
                    passwordEncoder.encode("test-customer-2-password"),
                    UserRole.CUSTOMER
            ));
        }
    }

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("test-customer", CUSTOMER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("test-customer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("test-customer", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ticketRoutesRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReadAnotherCustomersTicket() throws Exception {
        String customerToken = login("test-customer", CUSTOMER_PASSWORD);
        String otherCustomerToken = login("test-customer-2", "test-customer-2-password");

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Private customer issue",
                                  "description": "Only the owner should see this",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated());

        String ticketId = ticketRepository.findAll().get(0).getId().toString();

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportAgentCanReadAndChangeAnyTicket() throws Exception {
        String customerToken = login("test-customer", CUSTOMER_PASSWORD);
        String supportToken = login("test-support", SUPPORT_PASSWORD);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Support-managed issue",
                                  "description": "Support should manage this",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isCreated());

        String ticketId = ticketRepository.findAll().get(0).getId().toString();

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value("test-customer"));

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .header("Authorization", bearer(supportToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

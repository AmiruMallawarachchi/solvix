package com.solvix.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvix.backend.infrastructure.ticket.SpringDataTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTest {
    private static final String CUSTOMER_PASSWORD = "test-customer-password";
    private static final String SUPPORT_PASSWORD = "test-support-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataTicketRepository ticketRepository;

    @BeforeEach
    void cleanTickets() {
        ticketRepository.deleteAll();
    }

    @Test
    void customerCanCreateListAndCommentOnOwnTicket() throws Exception {
        String customerToken = login("test-customer", CUSTOMER_PASSWORD);
        String ticketId = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Payment issue",
                                  "description": "Payment completed but subscription is inactive",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Payment issue"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.createdBy").value("test-customer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(ticketId).get("id").asText();

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"I have attached the receipt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0]").value("I have attached the receipt"));
    }

    @Test
    void supportAgentCanAssignAndChangeTicketStatus() throws Exception {
        String customerToken = login("test-customer", CUSTOMER_PASSWORD);
        String supportToken = login("test-support", SUPPORT_PASSWORD);
        String id = createTicket(customerToken);

        mockMvc.perform(post("/api/v1/tickets/{id}/assign", id)
                        .header("Authorization", bearer(supportToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\":\"agent-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("agent-1"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", id)
                        .header("Authorization", bearer(supportToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void ticketRequestsRejectInvalidInputAndReportMissingTickets() throws Exception {
        String customerToken = login("test-customer", CUSTOMER_PASSWORD);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "description": "A valid description",
                                  "priority": "LOW"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/tickets/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void customerCannotAssignOrChangeStatus() throws Exception {
        String customerToken = login("test-customer", CUSTOMER_PASSWORD);
        String id = createTicket(customerToken);

        mockMvc.perform(post("/api/v1/tickets/{id}/assign", id)
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\":\"agent-1\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", id)
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());
    }

    private String createTicket(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test ticket",
                                  "description": "Ticket created for API integration testing",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

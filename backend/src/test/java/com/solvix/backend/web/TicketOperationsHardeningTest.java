package com.solvix.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import com.solvix.backend.web.ticket.TicketController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketOperationsHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create ticket with category returns version 0 and category")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void createTicketWithCategory() throws Exception {
        TicketController.CreateTicketRequest request = new TicketController.CreateTicketRequest(
                "Payment Failure Bug",
                "Unable to process credit card checkout",
                TicketPriority.HIGH,
                "Billing"
        );

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Payment Failure Bug"))
                .andExpect(jsonPath("$.category").value("Billing"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/tickets with page parameters returns paginated result structure")
    @WithMockUser(username = "agent1", roles = "SUPPORT_AGENT")
    void getPagedTickets() throws Exception {
        // Create 2 tickets
        TicketController.CreateTicketRequest req1 = new TicketController.CreateTicketRequest(
                "Database latency spike", "High CPU usage on RDS", TicketPriority.CRITICAL, "Infrastructure"
        );
        TicketController.CreateTicketRequest req2 = new TicketController.CreateTicketRequest(
                "UI alignment error", "Button off-center on mobile", TicketPriority.LOW, "Frontend"
        );

        mockMvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)));
        mockMvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "latency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Database latency spike"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("Optimistic lock: mismatched If-Match returns 409 Conflict")
    @WithMockUser(username = "agent1", roles = "SUPPORT_AGENT")
    void optimisticLockConflict() throws Exception {
        TicketController.CreateTicketRequest createReq = new TicketController.CreateTicketRequest(
                "Memory leak in worker node", "OutOfMemory error after 4 hours", TicketPriority.HIGH, "Backend"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        TicketController.TicketResponse createdTicket = objectMapper.readValue(
                result.getResponse().getContentAsString(), TicketController.TicketResponse.class
        );

        TicketController.ChangeStatusRequest statusReq = new TicketController.ChangeStatusRequest(TicketStatus.TRIAGED);

        // Mismatched version (e.g. version 999 instead of actual version 0)
        mockMvc.perform(patch("/api/v1/tickets/" + createdTicket.id() + "/status")
                        .header("If-Match", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}

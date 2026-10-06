package com.solvix.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.security.UserRole;
import com.solvix.backend.web.team.TeamController;
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
class TeamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Manager can create team and add members")
    @WithMockUser(username = "admin1", roles = "ADMINISTRATOR")
    void createTeamAndAddMembers() throws Exception {
        TeamController.CreateTeamRequest createReq = new TeamController.CreateTeamRequest(
                "Tier 2 Escalations", "Handles complex technical tickets"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tier 2 Escalations"))
                .andExpect(jsonPath("$.description").value("Handles complex technical tickets"))
                .andReturn();

        TeamController.TeamResponse team = objectMapper.readValue(
                result.getResponse().getContentAsString(), TeamController.TeamResponse.class
        );

        TeamController.AddMemberRequest memberReq = new TeamController.AddMemberRequest(
                "specialist1", UserRole.TEAM_LEAD
        );

        mockMvc.perform(post("/api/v1/teams/" + team.id() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].username").value("specialist1"))
                .andExpect(jsonPath("$.members[0].role").value("TEAM_LEAD"));
    }

    @Test
    @DisplayName("Customer cannot create teams")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void customerCannotCreateTeam() throws Exception {
        TeamController.CreateTeamRequest createReq = new TeamController.CreateTeamRequest(
                "Forbidden Team", "Test description"
        );

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Assign ticket to team")
    @WithMockUser(username = "agent1", roles = "SUPPORT_AGENT")
    void assignTicketToTeam() throws Exception {
        // Create team
        TeamController.CreateTeamRequest teamReq = new TeamController.CreateTeamRequest("DevOps Team", "Infrastructure support");
        MvcResult teamRes = mockMvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(teamReq)))
                .andExpect(status().isCreated()).andReturn();
        TeamController.TeamResponse team = objectMapper.readValue(teamRes.getResponse().getContentAsString(), TeamController.TeamResponse.class);

        // Create ticket
        TicketController.CreateTicketRequest ticketReq = new TicketController.CreateTicketRequest("Kubernetes node crash", "Node unreachable", TicketPriority.CRITICAL, "DevOps");
        MvcResult ticketRes = mockMvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ticketReq)))
                .andExpect(status().isCreated()).andReturn();
        TicketController.TicketResponse ticket = objectMapper.readValue(ticketRes.getResponse().getContentAsString(), TicketController.TicketResponse.class);

        // Assign to team
        TicketController.AssignTeamRequest assignTeamReq = new TicketController.AssignTeamRequest(team.id());
        mockMvc.perform(post("/api/v1/tickets/" + ticket.id() + "/assign-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignTeamReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(team.id().toString()));
    }
}

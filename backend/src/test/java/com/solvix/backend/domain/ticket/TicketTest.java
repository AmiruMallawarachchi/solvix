package com.solvix.backend.domain.ticket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTest {
    @Test
    void assigningNewTicketMovesItToAssigned() {
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "Payment issue",
                "Payment succeeded but subscription is inactive",
                TicketPriority.HIGH,
                "customer-1"
        );

        ticket.assignTo("agent-1");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(ticket.getAssignee()).isEqualTo("agent-1");
    }

    @Test
    void addingCommentStoresCommentOnTicket() {
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "Login issue",
                "Unable to log in",
                TicketPriority.MEDIUM,
                "customer-1"
        );

        ticket.addComment("Investigating the issue");

        assertThat(ticket.getComments()).containsExactly("Investigating the issue");
    }
}

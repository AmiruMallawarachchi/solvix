package com.solvix.backend.domain.ticket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {
    @Test
    void ticketCanBeAssignedAfterTriage() {
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "Payment issue",
                "Payment succeeded but subscription is inactive",
                TicketPriority.HIGH,
                "customer-1"
        );

        ticket.changeStatus(TicketStatus.TRIAGED);
        ticket.assignTo("agent-1");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TRIAGED);
        assertThat(ticket.getAssignee()).isEqualTo("agent-1");
    }

    @Test
    void addingCommentStoresTextAuthorAndTimestamp() {
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "Login issue",
                "Unable to log in",
                TicketPriority.MEDIUM,
                "customer-1"
        );

        ticket.addComment("Investigating the issue", "support-1");

        assertThat(ticket.getComments()).hasSize(1);
        assertThat(ticket.getComments().get(0).text()).isEqualTo("Investigating the issue");
        assertThat(ticket.getComments().get(0).author()).isEqualTo("support-1");
        assertThat(ticket.getComments().get(0).createdAt()).isNotNull();
    }

    @Test
    void enforcesTicketLifecycleAndAllowsReopeningResolvedTickets() {
        Ticket ticket = new Ticket(UUID.randomUUID(), "Payment issue", "Subscription inactive",
                TicketPriority.HIGH, "customer-1");

        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.ASSIGNED))
                .isInstanceOf(IllegalArgumentException.class);

        ticket.changeStatus(TicketStatus.TRIAGED);
        ticket.changeStatus(TicketStatus.ASSIGNED);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changeStatus(TicketStatus.RESOLVED);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void doesNotAllowTransitionsOutOfClosed() {
        Ticket ticket = new Ticket(UUID.randomUUID(), "Payment issue", "Subscription inactive",
                TicketPriority.HIGH, "customer-1");
        ticket.changeStatus(TicketStatus.TRIAGED);
        ticket.changeStatus(TicketStatus.ASSIGNED);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changeStatus(TicketStatus.RESOLVED);
        ticket.changeStatus(TicketStatus.CLOSED);

        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.IN_PROGRESS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ticketMustBeTriagedBeforeAssignment() {
        Ticket ticket = new Ticket(UUID.randomUUID(), "Payment issue", "Subscription inactive",
                TicketPriority.HIGH, "customer-1");

        assertThatThrownBy(() -> ticket.assignTo("agent-1"))
                .isInstanceOf(IllegalArgumentException.class);

        ticket.changeStatus(TicketStatus.TRIAGED);
        ticket.assignTo("agent-1");
        assertThat(ticket.getAssignee()).isEqualTo("agent-1");
    }
}

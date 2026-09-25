package com.solvix.backend.application.ticket;

import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketServiceAuthorizationTest {

    @Test
    void customerCanOnlyViewOwnTickets() {
        Ticket ticket = new Ticket(UUID.randomUUID(), "Payment issue", "Subscription inactive",
                TicketPriority.HIGH, "customer-1");
        TicketService service = new TicketService(new SingleTicketRepository(ticket));

        assertThat(service.findById(ticket.getId(), new TicketActor("customer-1", Set.of("CUSTOMER"))))
                .isSameAs(ticket);
        assertThatThrownBy(() -> service.findById(ticket.getId(),
                new TicketActor("customer-2", Set.of("CUSTOMER"))))
                .isInstanceOf(TicketAccessDeniedException.class);
    }

    @Test
    void supportAgentCanViewAndManageAnyTicket() {
        Ticket ticket = new Ticket(UUID.randomUUID(), "Payment issue", "Subscription inactive",
                TicketPriority.HIGH, "customer-1");
        TicketService service = new TicketService(new SingleTicketRepository(ticket));
        TicketActor supportAgent = new TicketActor("support-1", Set.of("SUPPORT_AGENT"));

        assertThat(service.findAll(supportAgent)).containsExactly(ticket);
        service.changeStatus(ticket.getId(), com.solvix.backend.domain.ticket.TicketStatus.TRIAGED, supportAgent);

        assertThat(ticket.getStatus()).isEqualTo(com.solvix.backend.domain.ticket.TicketStatus.TRIAGED);
    }

    private static final class SingleTicketRepository implements TicketRepository {
        private final Ticket ticket;

        private SingleTicketRepository(Ticket ticket) {
            this.ticket = ticket;
        }

        @Override
        public Ticket save(Ticket ticket) {
            return ticket;
        }

        @Override
        public Optional<Ticket> findById(UUID id) {
            return ticket.getId().equals(id) ? Optional.of(ticket) : Optional.empty();
        }

        @Override
        public List<Ticket> findAll() {
            return List.of(ticket);
        }
    }
}

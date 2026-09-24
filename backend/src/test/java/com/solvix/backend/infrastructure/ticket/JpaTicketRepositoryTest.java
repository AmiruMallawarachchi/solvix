package com.solvix.backend.infrastructure.ticket;

import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaTicketRepositoryTest {

    @Autowired
    private SpringDataTicketRepository springDataTicketRepository;

    @Test
    void shouldPersistAndLoadTicket() {
        JpaTicketRepository repository = new JpaTicketRepository(springDataTicketRepository);

        Ticket ticket = new Ticket(UUID.randomUUID(), "Payment issue", "Subscription inactive", TicketPriority.HIGH, "customer-1");
        ticket.assignTo("ops-agent-1");
        ticket.addComment("Investigating billing webhook");

        Ticket saved = repository.save(ticket);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findAll()).hasSize(1);

        Ticket loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getAssignee()).isEqualTo("ops-agent-1");
        assertThat(loaded.getComments()).containsExactly("Investigating billing webhook");
    }
}

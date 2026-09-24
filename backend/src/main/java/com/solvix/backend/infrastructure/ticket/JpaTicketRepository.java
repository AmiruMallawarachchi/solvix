package com.solvix.backend.infrastructure.ticket;

import com.solvix.backend.application.ticket.TicketRepository;
import com.solvix.backend.domain.ticket.Ticket;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTicketRepository implements TicketRepository {
    private final SpringDataTicketRepository delegate;

    public JpaTicketRepository(SpringDataTicketRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = TicketEntity.fromDomain(ticket);
        return delegate.save(entity).toDomain();
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        return delegate.findById(id).map(TicketEntity::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return delegate.findAll().stream().map(TicketEntity::toDomain).toList();
    }
}

package com.solvix.backend.application.ticket;

import com.solvix.backend.domain.ticket.Ticket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(UUID id);
    List<Ticket> findAll();
}

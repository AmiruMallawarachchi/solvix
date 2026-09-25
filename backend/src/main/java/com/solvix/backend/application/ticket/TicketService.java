package com.solvix.backend.application.ticket;

import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket create(String title, String description, TicketPriority priority, String createdBy) {
        Ticket ticket = new Ticket(UUID.randomUUID(), title, description, priority, createdBy);
        return ticketRepository.save(ticket);
    }

    public List<Ticket> findAll(TicketActor actor) {
        return ticketRepository.findAll().stream()
                .filter(ticket -> canAccess(actor, ticket))
                .toList();
    }

    public Ticket findById(UUID id, TicketActor actor) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        if (!canAccess(actor, ticket)) {
            throw new TicketAccessDeniedException(id);
        }
        return ticket;
    }

    public Ticket changeStatus(UUID id, TicketStatus status, TicketActor actor) {
        requireManager(actor);
        Ticket ticket = findById(id, actor);
        ticket.changeStatus(status);
        return ticketRepository.save(ticket);
    }

    public Ticket assign(UUID id, String assignee, TicketActor actor) {
        requireManager(actor);
        Ticket ticket = findById(id, actor);
        ticket.assignTo(assignee);
        return ticketRepository.save(ticket);
    }

    public Ticket addComment(UUID id, String comment, TicketActor actor) {
        Ticket ticket = findById(id, actor);
        ticket.addComment(comment);
        return ticketRepository.save(ticket);
    }

    private boolean canAccess(TicketActor actor, Ticket ticket) {
        return actor.canManageTickets() || ticket.getCreatedBy().equals(actor.username());
    }

    private void requireManager(TicketActor actor) {
        if (!actor.canManageTickets()) {
            throw new TicketAccessDeniedException(null);
        }
    }
}

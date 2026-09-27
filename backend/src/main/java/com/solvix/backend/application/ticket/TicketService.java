package com.solvix.backend.application.ticket;

import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketActivityRepository activityRepository;

    public TicketService(TicketRepository ticketRepository, TicketActivityRepository activityRepository) {
        this.ticketRepository = ticketRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public Ticket create(String title, String description, TicketPriority priority, String createdBy) {
        Ticket ticket = new Ticket(UUID.randomUUID(), title, description, priority, createdBy);
        Ticket saved = ticketRepository.save(ticket);
        record(saved, TicketActivityType.CREATED, createdBy, "Ticket created");
        return saved;
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

    @Transactional
    public Ticket changeStatus(UUID id, TicketStatus status, TicketActor actor) {
        requireManager(actor);
        Ticket ticket = findById(id, actor);
        TicketStatus previousStatus = ticket.getStatus();
        ticket.changeStatus(status);
        Ticket saved = ticketRepository.save(ticket);
        if (previousStatus != saved.getStatus()) {
            record(saved, TicketActivityType.STATUS_CHANGED, actor.username(),
                    "Status changed from " + previousStatus + " to " + saved.getStatus());
        }
        return saved;
    }

    @Transactional
    public Ticket assign(UUID id, String assignee, TicketActor actor) {
        requireManager(actor);
        Ticket ticket = findById(id, actor);
        TicketStatus previousStatus = ticket.getStatus();
        ticket.assignTo(assignee);
        Ticket saved = ticketRepository.save(ticket);
        record(saved, TicketActivityType.ASSIGNED, actor.username(), "Assigned to " + saved.getAssignee());
        if (previousStatus != saved.getStatus()) {
            record(saved, TicketActivityType.STATUS_CHANGED, actor.username(),
                    "Status changed from " + previousStatus + " to " + saved.getStatus());
        }
        return saved;
    }

    @Transactional
    public Ticket addComment(UUID id, String comment, TicketActor actor) {
        Ticket ticket = findById(id, actor);
        ticket.addComment(comment);
        Ticket saved = ticketRepository.save(ticket);
        record(saved, TicketActivityType.COMMENT_ADDED, actor.username(), "Comment added");
        return saved;
    }

    public List<TicketActivity> findHistory(UUID id, TicketActor actor) {
        findById(id, actor);
        return activityRepository.findByTicketIdOrderedByCreatedAt(id);
    }

    private boolean canAccess(TicketActor actor, Ticket ticket) {
        return actor.canManageTickets() || ticket.getCreatedBy().equals(actor.username());
    }

    private void requireManager(TicketActor actor) {
        if (!actor.canManageTickets()) {
            throw new TicketAccessDeniedException(null);
        }
    }

    private void record(Ticket ticket, TicketActivityType type, String actor, String description) {
        activityRepository.save(new TicketActivity(
                UUID.randomUUID(),
                ticket.getId(),
                type,
                actor,
                description,
                java.time.Instant.now()
        ));
    }
}

package com.solvix.backend.application.ticket;

import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private static final int MAX_PAGE_SIZE = 50;

    private final TicketRepository ticketRepository;
    private final TicketActivityRepository activityRepository;

    public TicketService(TicketRepository ticketRepository, TicketActivityRepository activityRepository) {
        this.ticketRepository = ticketRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public Ticket create(String title, String description, TicketPriority priority, String createdBy) {
        return create(title, description, priority, null, createdBy);
    }

    @Transactional
    public Ticket create(String title, String description, TicketPriority priority, String category, String createdBy) {
        Ticket ticket = new Ticket(UUID.randomUUID(), title, description, priority, createdBy, category);
        Ticket saved = ticketRepository.save(ticket);
        record(saved, TicketActivityType.CREATED, createdBy, "Ticket created");
        return saved;
    }

    public List<Ticket> findAll(TicketActor actor) {
        return ticketRepository.findAll().stream()
                .filter(ticket -> canAccess(actor, ticket))
                .toList();
    }

    public Page<Ticket> findPaged(
            TicketActor actor,
            Pageable pageable,
            String search,
            TicketStatus status,
            TicketPriority priority,
            String category
    ) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable cappedPageable = PageRequest.of(pageable.getPageNumber(), pageSize, sort);

        String createdByFilter = actor.canManageTickets() ? null : actor.username();
        return ticketRepository.findPaged(cappedPageable, search, status, priority, category, createdByFilter);
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
        return changeStatus(id, status, null, actor);
    }

    @Transactional
    public Ticket changeStatus(UUID id, TicketStatus status, Long expectedVersion, TicketActor actor) {
        requireManager(actor);
        Ticket ticket = findById(id, actor);
        verifyVersion(ticket, expectedVersion);
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
        return assign(id, assignee, null, actor);
    }

    @Transactional
    public Ticket assign(UUID id, String assignee, Long expectedVersion, TicketActor actor) {
        requireManager(actor);
        Ticket ticket = findById(id, actor);
        verifyVersion(ticket, expectedVersion);
        ticket.assignTo(assignee);
        Ticket saved = ticketRepository.save(ticket);
        record(saved, TicketActivityType.ASSIGNED, actor.username(), "Assigned to " + saved.getAssignee());
        return saved;
    }

    @Transactional
    public Ticket addComment(UUID id, String comment, TicketActor actor) {
        return addComment(id, comment, null, actor);
    }

    @Transactional
    public Ticket addComment(UUID id, String comment, Long expectedVersion, TicketActor actor) {
        Ticket ticket = findById(id, actor);
        verifyVersion(ticket, expectedVersion);
        ticket.addComment(comment, actor.username());
        Ticket saved = ticketRepository.save(ticket);
        record(saved, TicketActivityType.COMMENT_ADDED, actor.username(), "Comment added");
        return saved;
    }

    public List<TicketActivity> findHistory(UUID id, TicketActor actor) {
        findById(id, actor);
        return activityRepository.findByTicketIdOrderedByCreatedAt(id);
    }

    private void verifyVersion(Ticket ticket, Long expectedVersion) {
        if (expectedVersion != null && ticket.getVersion() != null && !expectedVersion.equals(ticket.getVersion())) {
            throw new OptimisticLockConflictException(ticket.getId(), expectedVersion, ticket.getVersion());
        }
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

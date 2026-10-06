package com.solvix.backend.domain.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Ticket {
    private final UUID id;
    private final String title;
    private final String description;
    private final String createdBy;
    private final Instant createdAt;
    private final List<TicketComment> comments = new ArrayList<>();
    private TicketPriority priority;
    private TicketStatus status;
    private String assignee;
    private UUID teamId;
    private String category;
    private Long version;
    private Instant updatedAt;

    public Ticket(UUID id, String title, String description, TicketPriority priority, String createdBy) {
        this(id, title, description, priority, createdBy, null, TicketStatus.NEW, null, null, Instant.now(), Instant.now(), null, List.of());
    }

    public Ticket(UUID id, String title, String description, TicketPriority priority, String createdBy, String category) {
        this(id, title, description, priority, createdBy, category, TicketStatus.NEW, null, null, Instant.now(), Instant.now(), null, List.of());
    }

    private Ticket(
            UUID id,
            String title,
            String description,
            TicketPriority priority,
            String createdBy,
            String category,
            TicketStatus status,
            String assignee,
            UUID teamId,
            Instant createdAt,
            Instant updatedAt,
            Long version,
            List<TicketComment> comments
    ) {
        this.id = Objects.requireNonNull(id);
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.priority = Objects.requireNonNull(priority);
        this.createdBy = requireText(createdBy, "createdBy");
        this.category = category;
        this.status = status == null ? TicketStatus.NEW : status;
        this.assignee = assignee;
        this.teamId = teamId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.version = version;
        if (comments != null) {
            this.comments.addAll(comments);
        }
    }

    public static Ticket fromPersistence(
            UUID id,
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status,
            String createdBy,
            String assignee,
            UUID teamId,
            String category,
            Long version,
            Instant createdAt,
            Instant updatedAt,
            List<TicketComment> comments
    ) {
        return new Ticket(id, title, description, priority, createdBy, category, status, assignee, teamId, createdAt, updatedAt, version, comments);
    }

    public void changeStatus(TicketStatus newStatus) {
        Objects.requireNonNull(newStatus);
        if (status == newStatus) {
            return;
        }
        if (!canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Cannot change ticket status from " + status + " to " + newStatus);
        }
        this.status = newStatus;
        touch();
    }

    public void assignTo(String assignee) {
        if (status != TicketStatus.TRIAGED
                && status != TicketStatus.ASSIGNED
                && status != TicketStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Ticket must be triaged before it can be assigned");
        }
        this.assignee = requireText(assignee, "assignee");
        touch();
    }

    public void assignTeam(UUID teamId) {
        this.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
        touch();
    }

    public void addComment(String comment, String author) {
        comments.add(new TicketComment(null, comment, author, Instant.now()));
        touch();
    }

    private boolean canTransitionTo(TicketStatus newStatus) {
        return switch (status) {
            case NEW -> newStatus == TicketStatus.TRIAGED;
            case TRIAGED -> newStatus == TicketStatus.ASSIGNED;
            case ASSIGNED -> newStatus == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED;
            case RESOLVED -> newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketPriority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public String getAssignee() { return assignee; }
    public UUID getTeamId() { return teamId; }
    public String getCategory() { return category; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TicketComment> getComments() { return Collections.unmodifiableList(comments); }
}

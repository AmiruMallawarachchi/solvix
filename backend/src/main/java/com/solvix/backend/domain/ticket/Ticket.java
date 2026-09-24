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
    private final List<String> comments = new ArrayList<>();
    private TicketPriority priority;
    private TicketStatus status;
    private String assignee;
    private Instant updatedAt;

    public Ticket(UUID id, String title, String description, TicketPriority priority, String createdBy) {
        this(id, title, description, priority, createdBy, TicketStatus.NEW, null, Instant.now(), Instant.now(), List.of());
    }

    private Ticket(
            UUID id,
            String title,
            String description,
            TicketPriority priority,
            String createdBy,
            TicketStatus status,
            String assignee,
            Instant createdAt,
            Instant updatedAt,
            List<String> comments
    ) {
        this.id = Objects.requireNonNull(id);
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.priority = Objects.requireNonNull(priority);
        this.createdBy = requireText(createdBy, "createdBy");
        this.status = status == null ? TicketStatus.NEW : status;
        this.assignee = assignee;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        if (comments != null) {
            this.comments.addAll(comments.stream().map(comment -> requireText(comment, "comment")).toList());
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
            Instant createdAt,
            Instant updatedAt,
            List<String> comments
    ) {
        return new Ticket(id, title, description, priority, createdBy, status, assignee, createdAt, updatedAt, comments);
    }

    public void changeStatus(TicketStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus);
        touch();
    }

    public void assignTo(String assignee) {
        this.assignee = requireText(assignee, "assignee");
        if (status == TicketStatus.NEW || status == TicketStatus.TRIAGED) {
            status = TicketStatus.ASSIGNED;
        }
        touch();
    }

    public void addComment(String comment) {
        comments.add(requireText(comment, "comment"));
        touch();
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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<String> getComments() { return Collections.unmodifiableList(comments); }
}

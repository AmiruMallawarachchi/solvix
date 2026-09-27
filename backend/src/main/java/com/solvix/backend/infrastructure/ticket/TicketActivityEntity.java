package com.solvix.backend.infrastructure.ticket;

import com.solvix.backend.application.ticket.TicketActivity;
import com.solvix.backend.application.ticket.TicketActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_activity")
public class TicketActivityEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private TicketEntity ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private TicketActivityType type;

    @Column(nullable = false, updatable = false)
    private String actor;

    @Column(nullable = false, updatable = false, length = 1000)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketActivityEntity() {
    }

    public TicketActivityEntity(TicketActivity activity, TicketEntity ticket) {
        this.id = activity.id();
        this.ticket = ticket;
        this.type = activity.type();
        this.actor = activity.actor();
        this.description = activity.description();
        this.createdAt = activity.createdAt();
    }

    public TicketActivity toApplication() {
        return new TicketActivity(id, ticket.getId(), type, actor, description, createdAt);
    }
}

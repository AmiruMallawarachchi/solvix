package com.solvix.backend.infrastructure.ticket;

import com.solvix.backend.domain.ticket.TicketComment;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ticket_comments")
public class TicketCommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private TicketEntity ticket;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketCommentEntity() {
    }

    public TicketCommentEntity(TicketComment comment, TicketEntity ticket) {
        this.id = comment.id();
        this.ticket = ticket;
        this.body = comment.text();
        this.author = comment.author();
        this.createdAt = comment.createdAt();
    }

    public TicketComment toDomain() {
        return new TicketComment(id, body, author, createdAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }
}

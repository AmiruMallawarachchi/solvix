package com.solvix.backend.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_admin_audit")
public class UserAdministrationAuditEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Column(name = "target_username", nullable = false)
    private String targetUsername;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAdministrationAuditEntity() {
    }

    public UserAdministrationAuditEntity(
            UUID id,
            String actorUsername,
            String targetUsername,
            String action,
            Instant createdAt
    ) {
        this.id = id;
        this.actorUsername = actorUsername;
        this.targetUsername = targetUsername;
        this.action = action;
        this.createdAt = createdAt;
    }
}

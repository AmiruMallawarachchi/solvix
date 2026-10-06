package com.solvix.backend.infrastructure.team;

import com.solvix.backend.domain.team.TeamMember;
import com.solvix.backend.security.UserRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_memberships")
public class TeamMembershipEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected TeamMembershipEntity() {
    }

    public TeamMembershipEntity(UUID id, TeamEntity team, String username, UserRole role, Instant joinedAt) {
        this.id = id;
        this.team = team;
        this.username = username;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public TeamMember toDomain() {
        return new TeamMember(id, team.getId(), username, role, joinedAt);
    }

    public UUID getId() { return id; }
    public TeamEntity getTeam() { return team; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}

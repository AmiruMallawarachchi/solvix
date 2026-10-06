package com.solvix.backend.infrastructure.team;

import com.solvix.backend.domain.team.Team;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "teams")
public class TeamEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TeamMembershipEntity> members = new ArrayList<>();

    protected TeamEntity() {
    }

    public TeamEntity(UUID id, String name, String description, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static TeamEntity fromDomain(Team team) {
        TeamEntity entity = new TeamEntity(team.getId(), team.getName(), team.getDescription(), team.getCreatedAt());
        entity.members = team.getMembers().stream()
                .map(m -> new TeamMembershipEntity(m.id(), entity, m.username(), m.role(), m.joinedAt()))
                .collect(Collectors.toCollection(ArrayList::new));
        return entity;
    }

    public Team toDomain() {
        return Team.fromPersistence(
                id,
                name,
                description,
                createdAt,
                members.stream().map(TeamMembershipEntity::toDomain).toList()
        );
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TeamMembershipEntity> getMembers() { return members; }
}

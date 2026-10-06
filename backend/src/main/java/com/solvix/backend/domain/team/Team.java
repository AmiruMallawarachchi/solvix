package com.solvix.backend.domain.team;

import com.solvix.backend.security.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Team {
    private final UUID id;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final List<TeamMember> members = new ArrayList<>();

    public Team(UUID id, String name, String description) {
        this(id, name, description, Instant.now(), List.of());
    }

    private Team(UUID id, String name, String description, Instant createdAt, List<TeamMember> members) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = requireText(name, "name");
        this.description = description;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        if (members != null) {
            this.members.addAll(members);
        }
    }

    public static Team fromPersistence(UUID id, String name, String description, Instant createdAt, List<TeamMember> members) {
        return new Team(id, name, description, createdAt, members);
    }

    public void addMember(String username, UserRole role) {
        requireText(username, "username");
        Objects.requireNonNull(role, "role must not be null");
        boolean exists = members.stream().anyMatch(m -> m.username().equalsIgnoreCase(username.trim()));
        if (exists) {
            throw new IllegalArgumentException("User " + username + " is already a member of team " + name);
        }
        members.add(new TeamMember(UUID.randomUUID(), id, username.trim(), role, Instant.now()));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TeamMember> getMembers() { return Collections.unmodifiableList(members); }
}

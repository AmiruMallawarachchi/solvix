package com.solvix.backend.domain.team;

import com.solvix.backend.security.UserRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TeamMember(
        UUID id,
        UUID teamId,
        String username,
        UserRole role,
        Instant joinedAt
) {
    public TeamMember {
        Objects.requireNonNull(teamId, "teamId must not be null");
        Objects.requireNonNull(username, "username must not be blank");
        Objects.requireNonNull(role, "role must not be null");
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}

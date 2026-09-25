package com.solvix.backend.application.ticket;

import java.util.Set;

public record TicketActor(String username, Set<String> roles) {
    public TicketActor {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean canManageTickets() {
        return hasRole("SUPPORT_AGENT")
                || hasRole("DEVELOPER")
                || hasRole("TEAM_LEAD")
                || hasRole("MANAGER")
                || hasRole("ADMINISTRATOR");
    }
}

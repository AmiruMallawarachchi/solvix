package com.solvix.backend.application.ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketActivity(
        UUID id,
        UUID ticketId,
        TicketActivityType type,
        String actor,
        String description,
        Instant createdAt
) {
}

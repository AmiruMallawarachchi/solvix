package com.solvix.backend.application.ticket;

import java.util.List;
import java.util.UUID;

public interface TicketActivityRepository {
    TicketActivity save(TicketActivity activity);
    List<TicketActivity> findByTicketIdOrderedByCreatedAt(UUID ticketId);
}

package com.solvix.backend.application.ticket;

import java.util.UUID;

public class TicketAccessDeniedException extends RuntimeException {
    public TicketAccessDeniedException(UUID ticketId) {
        super(ticketId == null
                ? "The current user is not allowed to perform this action"
                : "The current user is not allowed to access ticket " + ticketId);
    }
}

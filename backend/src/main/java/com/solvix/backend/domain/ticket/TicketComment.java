package com.solvix.backend.domain.ticket;

import java.time.Instant;

public record TicketComment(Long id, String text, String author, Instant createdAt) {
    public TicketComment {
        text = requireText(text, "comment");
        author = requireText(author, "author");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}

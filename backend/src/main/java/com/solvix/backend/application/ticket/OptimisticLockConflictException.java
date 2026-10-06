package com.solvix.backend.application.ticket;

import java.util.UUID;

public class OptimisticLockConflictException extends RuntimeException {
    public OptimisticLockConflictException(UUID ticketId, Long expectedVersion, Long actualVersion) {
        super("Conflict for ticket " + ticketId + ": expected version " + expectedVersion + " but current version is " + actualVersion);
    }
}

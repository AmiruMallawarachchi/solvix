package com.solvix.backend.infrastructure.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataTicketActivityRepository extends JpaRepository<TicketActivityEntity, UUID> {
    List<TicketActivityEntity> findByTicket_IdOrderByCreatedAtAscIdAsc(UUID ticketId);
}

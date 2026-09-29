package com.solvix.backend.infrastructure.ticket;

import com.solvix.backend.application.ticket.TicketActivity;
import com.solvix.backend.application.ticket.TicketActivityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaTicketActivityRepository implements TicketActivityRepository {
    private final SpringDataTicketActivityRepository activities;
    private final SpringDataTicketRepository tickets;

    public JpaTicketActivityRepository(
            SpringDataTicketActivityRepository activities,
            SpringDataTicketRepository tickets
    ) {
        this.activities = activities;
        this.tickets = tickets;
    }

    @Override
    public TicketActivity save(TicketActivity activity) {
        TicketEntity ticket = tickets.getReferenceById(activity.ticketId());
        return activities.save(new TicketActivityEntity(activity, ticket)).toApplication();
    }

    @Override
    public List<TicketActivity> findByTicketIdOrderedByCreatedAt(UUID ticketId) {
        return activities.findByTicket_IdOrderByCreatedAtAscIdAsc(ticketId)
                .stream()
                .map(TicketActivityEntity::toApplication)
                .toList();
    }
}

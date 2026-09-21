package com.solvix.backend.application.ticket;

import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket create(String title, String description, TicketPriority priority, String createdBy) {
        Ticket ticket = new Ticket(UUID.randomUUID(), title, description, priority, createdBy);
        return ticketRepository.save(ticket);
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public Ticket findById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Ticket changeStatus(UUID id, TicketStatus status) {
        Ticket ticket = findById(id);
        ticket.changeStatus(status);
        return ticketRepository.save(ticket);
    }

    public Ticket assign(UUID id, String assignee) {
        Ticket ticket = findById(id);
        ticket.assignTo(assignee);
        return ticketRepository.save(ticket);
    }

    public Ticket addComment(UUID id, String comment) {
        Ticket ticket = findById(id);
        ticket.addComment(comment);
        return ticketRepository.save(ticket);
    }
}

package com.solvix.backend.web.ticket;

import com.solvix.backend.application.ticket.TicketService;
import com.solvix.backend.domain.ticket.Ticket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return TicketResponse.from(ticketService.create(
                request.title(),
                request.description(),
                request.priority(),
                request.createdBy()
        ));
    }

    @GetMapping
    public List<TicketResponse> findAll() {
        return ticketService.findAll().stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable UUID id) {
        return TicketResponse.from(ticketService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request
    ) {
        return TicketResponse.from(ticketService.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignTicketRequest request) {
        return TicketResponse.from(ticketService.assign(id, request.assignee()));
    }

    @PostMapping("/{id}/comments")
    public TicketResponse addComment(@PathVariable UUID id, @Valid @RequestBody AddCommentRequest request) {
        return TicketResponse.from(ticketService.addComment(id, request.comment()));
    }

    public record CreateTicketRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotNull com.solvix.backend.domain.ticket.TicketPriority priority,
            @NotBlank String createdBy
    ) {}

    public record ChangeStatusRequest(
            @NotNull com.solvix.backend.domain.ticket.TicketStatus status
    ) {}

    public record AssignTicketRequest(@NotBlank String assignee) {}

    public record AddCommentRequest(@NotBlank String comment) {}

    public record TicketResponse(
            UUID id,
            String title,
            String description,
            com.solvix.backend.domain.ticket.TicketPriority priority,
            com.solvix.backend.domain.ticket.TicketStatus status,
            String createdBy,
            String assignee,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            List<String> comments
    ) {
        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getPriority(),
                    ticket.getStatus(),
                    ticket.getCreatedBy(),
                    ticket.getAssignee(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt(),
                    ticket.getComments()
            );
        }
    }
}

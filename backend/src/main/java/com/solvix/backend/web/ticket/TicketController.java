package com.solvix.backend.web.ticket;

import com.solvix.backend.application.ticket.TicketService;
import com.solvix.backend.domain.ticket.Ticket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request, Authentication authentication) {
        return TicketResponse.from(ticketService.create(
                request.title(),
                request.description(),
                request.priority(),
                authentication.getName()
        ));
    }

    @GetMapping
    public List<TicketResponse> findAll(Authentication authentication) {
        return ticketService.findAll(actor(authentication)).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable UUID id, Authentication authentication) {
        return TicketResponse.from(ticketService.findById(id, actor(authentication)));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication
    ) {
        return TicketResponse.from(ticketService.changeStatus(id, request.status(), actor(authentication)));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignTicketRequest request,
                                 Authentication authentication) {
        return TicketResponse.from(ticketService.assign(id, request.assignee(), actor(authentication)));
    }

    @PostMapping("/{id}/comments")
    public TicketResponse addComment(@PathVariable UUID id, @Valid @RequestBody AddCommentRequest request,
                                     Authentication authentication) {
        return TicketResponse.from(ticketService.addComment(id, request.comment(), actor(authentication)));
    }

    public record CreateTicketRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotNull com.solvix.backend.domain.ticket.TicketPriority priority
    ) {}

    public record ChangeStatusRequest(
            @NotNull com.solvix.backend.domain.ticket.TicketStatus status
    ) {}

    public record AssignTicketRequest(@NotBlank String assignee) {}

    public record AddCommentRequest(@NotBlank String comment) {}

    private static com.solvix.backend.application.ticket.TicketActor actor(Authentication authentication) {
        return new com.solvix.backend.application.ticket.TicketActor(
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

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

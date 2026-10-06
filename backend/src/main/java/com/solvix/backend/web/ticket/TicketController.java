package com.solvix.backend.web.ticket;

import com.solvix.backend.application.ticket.TicketService;
import com.solvix.backend.application.ticket.TicketActivity;
import com.solvix.backend.domain.ticket.TicketComment;
import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                request.category(),
                authentication.getName()
        ));
    }

    @GetMapping
    public Object findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String category,
            Authentication authentication
    ) {
        if (page == null && search == null && status == null && priority == null && category == null) {
            return ticketService.findAll(actor(authentication)).stream().map(TicketResponse::from).toList();
        }

        int pageNum = page != null ? page : 0;
        String[] sortParts = sort.split(",");
        String sortProperty = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by(direction, sortProperty));
        Page<Ticket> ticketPage = ticketService.findPaged(actor(authentication), pageable, search, status, priority, category);

        return new PageResponse<>(
                ticketPage.getContent().stream().map(TicketResponse::from).toList(),
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable UUID id, Authentication authentication) {
        return TicketResponse.from(ticketService.findById(id, actor(authentication)));
    }

    @GetMapping("/{id}/history")
    public List<ActivityResponse> findHistory(@PathVariable UUID id, Authentication authentication) {
        return ticketService.findHistory(id, actor(authentication)).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            Authentication authentication
    ) {
        Long expectedVersion = parseVersionHeader(ifMatch);
        return TicketResponse.from(ticketService.changeStatus(id, request.status(), expectedVersion, actor(authentication)));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTicketRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            Authentication authentication
    ) {
        Long expectedVersion = parseVersionHeader(ifMatch);
        return TicketResponse.from(ticketService.assign(id, request.assignee(), expectedVersion, actor(authentication)));
    }

    @PostMapping("/{id}/assign-team")
    public TicketResponse assignTeam(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTeamRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            Authentication authentication
    ) {
        Long expectedVersion = parseVersionHeader(ifMatch);
        return TicketResponse.from(ticketService.assignTeam(id, request.teamId(), expectedVersion, actor(authentication)));
    }

    @PostMapping("/{id}/comments")
    public TicketResponse addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            Authentication authentication
    ) {
        Long expectedVersion = parseVersionHeader(ifMatch);
        return TicketResponse.from(ticketService.addComment(id, request.comment(), expectedVersion, actor(authentication)));
    }

    private Long parseVersionHeader(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            String clean = header.replace("\"", "").trim();
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record CreateTicketRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotNull com.solvix.backend.domain.ticket.TicketPriority priority,
            String category
    ) {}

    public record ChangeStatusRequest(
            @NotNull com.solvix.backend.domain.ticket.TicketStatus status
    ) {}

    public record AssignTicketRequest(@NotBlank String assignee) {}

    public record AssignTeamRequest(@NotNull UUID teamId) {}

    public record AddCommentRequest(@NotBlank String comment) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record CommentResponse(Long id, String text, String author, java.time.Instant createdAt) {
        static CommentResponse from(TicketComment comment) {
            return new CommentResponse(comment.id(), comment.text(), comment.author(), comment.createdAt());
        }
    }

    public record ActivityResponse(
            UUID id,
            UUID ticketId,
            String type,
            String actor,
            String description,
            java.time.Instant createdAt
    ) {
        static ActivityResponse from(TicketActivity activity) {
            return new ActivityResponse(
                    activity.id(),
                    activity.ticketId(),
                    activity.type().name(),
                    activity.actor(),
                    activity.description(),
                    activity.createdAt()
            );
        }
    }

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
            UUID teamId,
            String category,
            Long version,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            List<CommentResponse> comments
    ) {
        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getPriority(),
                    ticket.getStatus(),
                    ticket.getCreatedBy(),
                    ticket.getAssignee(),
                    ticket.getTeamId(),
                    ticket.getCategory(),
                    ticket.getVersion() != null ? ticket.getVersion() : 0L,
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt(),
                    ticket.getComments().stream().map(CommentResponse::from).toList()
            );
        }
    }
}

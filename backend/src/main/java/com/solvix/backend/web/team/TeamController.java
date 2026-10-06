package com.solvix.backend.web.team;

import com.solvix.backend.application.team.TeamService;
import com.solvix.backend.domain.team.Team;
import com.solvix.backend.domain.team.TeamMember;
import com.solvix.backend.security.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMINISTRATOR')")
    public TeamResponse createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return TeamResponse.from(teamService.createTeam(request.name(), request.description()));
    }

    @GetMapping
    public List<TeamResponse> findAll() {
        return teamService.findAll().stream().map(TeamResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TeamResponse findById(@PathVariable UUID id) {
        return TeamResponse.from(teamService.findById(id));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMINISTRATOR', 'TEAM_LEAD')")
    public TeamResponse addMember(@PathVariable UUID id, @Valid @RequestBody AddMemberRequest request) {
        return TeamResponse.from(teamService.addMember(id, request.username(), request.role()));
    }

    public record CreateTeamRequest(
            @NotBlank String name,
            String description
    ) {}

    public record AddMemberRequest(
            @NotBlank String username,
            @NotNull UserRole role
    ) {}

    public record MemberResponse(UUID id, UUID teamId, String username, UserRole role, Instant joinedAt) {
        public static MemberResponse from(TeamMember member) {
            return new MemberResponse(member.id(), member.teamId(), member.username(), member.role(), member.joinedAt());
        }
    }

    public record TeamResponse(
            UUID id,
            String name,
            String description,
            Instant createdAt,
            List<MemberResponse> members
    ) {
        public static TeamResponse from(Team team) {
            return new TeamResponse(
                    team.getId(),
                    team.getName(),
                    team.getDescription(),
                    team.getCreatedAt(),
                    team.getMembers().stream().map(MemberResponse::from).toList()
            );
        }
    }
}

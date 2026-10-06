package com.solvix.backend.application.team;

import com.solvix.backend.domain.team.Team;
import com.solvix.backend.security.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {
    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Team createTeam(String name, String description) {
        if (teamRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Team with name '" + name + "' already exists");
        }
        Team team = new Team(UUID.randomUUID(), name, description);
        return teamRepository.save(team);
    }

    public List<Team> findAll() {
        return teamRepository.findAll();
    }

    public Team findById(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + id));
    }

    @Transactional
    public Team addMember(UUID teamId, String username, UserRole role) {
        Team team = findById(teamId);
        team.addMember(username, role);
        return teamRepository.save(team);
    }
}

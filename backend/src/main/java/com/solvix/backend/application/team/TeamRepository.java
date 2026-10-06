package com.solvix.backend.application.team;

import com.solvix.backend.domain.team.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    Optional<Team> findByName(String name);
    List<Team> findAll();
}

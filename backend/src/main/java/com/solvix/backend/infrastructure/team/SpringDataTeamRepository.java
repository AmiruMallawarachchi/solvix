package com.solvix.backend.infrastructure.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataTeamRepository extends JpaRepository<TeamEntity, UUID> {
    Optional<TeamEntity> findByNameIgnoreCase(String name);
}

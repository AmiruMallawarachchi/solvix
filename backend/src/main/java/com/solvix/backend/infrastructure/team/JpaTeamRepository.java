package com.solvix.backend.infrastructure.team;

import com.solvix.backend.application.team.TeamRepository;
import com.solvix.backend.domain.team.Team;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTeamRepository implements TeamRepository {
    private final SpringDataTeamRepository delegate;

    public JpaTeamRepository(SpringDataTeamRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Team save(Team team) {
        TeamEntity entity = TeamEntity.fromDomain(team);
        return delegate.save(entity).toDomain();
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return delegate.findById(id).map(TeamEntity::toDomain);
    }

    @Override
    public Optional<Team> findByName(String name) {
        return delegate.findByNameIgnoreCase(name).map(TeamEntity::toDomain);
    }

    @Override
    public List<Team> findAll() {
        return delegate.findAll().stream().map(TeamEntity::toDomain).toList();
    }
}

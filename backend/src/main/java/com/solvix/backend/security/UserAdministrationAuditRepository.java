package com.solvix.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAdministrationAuditRepository extends JpaRepository<UserAdministrationAuditEntity, UUID> {
}

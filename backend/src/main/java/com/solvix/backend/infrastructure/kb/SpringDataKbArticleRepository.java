package com.solvix.backend.infrastructure.kb;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SpringDataKbArticleRepository extends JpaRepository<KbArticleEntity, UUID>, JpaSpecificationExecutor<KbArticleEntity> {
}

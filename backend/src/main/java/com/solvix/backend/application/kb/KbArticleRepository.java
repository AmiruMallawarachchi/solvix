package com.solvix.backend.application.kb;

import com.solvix.backend.domain.kb.ArticleStatus;
import com.solvix.backend.domain.kb.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface KbArticleRepository {
    KbArticle save(KbArticle article);
    Optional<KbArticle> findById(UUID id);
    Page<KbArticle> findPaged(Pageable pageable, String search, ArticleStatus status, String category, String tag);
}

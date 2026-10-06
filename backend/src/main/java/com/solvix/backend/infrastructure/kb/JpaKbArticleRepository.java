package com.solvix.backend.infrastructure.kb;

import com.solvix.backend.application.kb.KbArticleRepository;
import com.solvix.backend.domain.kb.ArticleStatus;
import com.solvix.backend.domain.kb.KbArticle;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaKbArticleRepository implements KbArticleRepository {
    private final SpringDataKbArticleRepository delegate;

    public JpaKbArticleRepository(SpringDataKbArticleRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public KbArticle save(KbArticle article) {
        KbArticleEntity entity = KbArticleEntity.fromDomain(article);
        return delegate.save(entity).toDomain();
    }

    @Override
    public Optional<KbArticle> findById(UUID id) {
        return delegate.findById(id).map(KbArticleEntity::toDomain);
    }

    @Override
    public Page<KbArticle> findPaged(Pageable pageable, String search, ArticleStatus status, String category, String tag) {
        Specification<KbArticleEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase().trim()));
            }
            if (tag != null && !tag.isBlank()) {
                predicates.add(cb.isMember(tag.toLowerCase().trim(), root.get("tags")));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase().trim() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), term);
                Predicate contentLike = cb.like(cb.lower(root.get("content")), term);
                predicates.add(cb.or(titleLike, contentLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return delegate.findAll(spec, pageable).map(KbArticleEntity::toDomain);
    }
}

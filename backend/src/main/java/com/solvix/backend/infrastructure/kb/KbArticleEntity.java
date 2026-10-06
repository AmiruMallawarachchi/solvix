package com.solvix.backend.infrastructure.kb;

import com.solvix.backend.domain.kb.ArticleStatus;
import com.solvix.backend.domain.kb.KbArticle;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "kb_articles")
public class KbArticleEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus status;

    @Column(nullable = false)
    private String author;

    private String category;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant publishedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kb_article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    protected KbArticleEntity() {
    }

    public KbArticleEntity(
            UUID id,
            String title,
            String content,
            ArticleStatus status,
            String author,
            String category,
            int viewCount,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Set<String> tags
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.status = status;
        this.author = author;
        this.category = category;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public static KbArticleEntity fromDomain(KbArticle article) {
        return new KbArticleEntity(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getStatus(),
                article.getAuthor(),
                article.getCategory(),
                article.getViewCount(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getPublishedAt(),
                article.getTags()
        );
    }

    public KbArticle toDomain() {
        return KbArticle.fromPersistence(
                id,
                title,
                content,
                status,
                author,
                category,
                viewCount,
                createdAt,
                updatedAt,
                publishedAt,
                tags
        );
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public ArticleStatus getStatus() { return status; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getViewCount() { return viewCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Set<String> getTags() { return tags; }
}

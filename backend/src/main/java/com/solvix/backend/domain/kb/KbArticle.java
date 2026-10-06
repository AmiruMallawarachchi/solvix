package com.solvix.backend.domain.kb;

import java.time.Instant;
import java.util.*;

public class KbArticle {
    private final UUID id;
    private final String title;
    private final String content;
    private final String author;
    private final String category;
    private final Set<String> tags = new HashSet<>();
    private final Instant createdAt;
    private ArticleStatus status;
    private int viewCount;
    private Instant updatedAt;
    private Instant publishedAt;

    public KbArticle(UUID id, String title, String content, String author, String category, Set<String> tags) {
        this(id, title, content, ArticleStatus.DRAFT, author, category, 0, Instant.now(), Instant.now(), null, tags);
    }

    private KbArticle(
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
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = requireText(title, "title");
        this.content = requireText(content, "content");
        this.author = requireText(author, "author");
        this.category = category;
        this.status = status == null ? ArticleStatus.DRAFT : status;
        this.viewCount = Math.max(0, viewCount);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.publishedAt = publishedAt;
        if (tags != null) {
            tags.stream().filter(t -> t != null && !t.isBlank()).forEach(t -> this.tags.add(t.trim().toLowerCase()));
        }
    }

    public static KbArticle fromPersistence(
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
        return new KbArticle(id, title, content, status, author, category, viewCount, createdAt, updatedAt, publishedAt, tags);
    }

    public void publish() {
        if (status == ArticleStatus.PUBLISHED) {
            return;
        }
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        touch();
    }

    public void archive() {
        this.status = ArticleStatus.ARCHIVED;
        touch();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
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
    public Set<String> getTags() { return Collections.unmodifiableSet(tags); }
}

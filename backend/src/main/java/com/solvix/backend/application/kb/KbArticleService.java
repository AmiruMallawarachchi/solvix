package com.solvix.backend.application.kb;

import com.solvix.backend.domain.kb.ArticleStatus;
import com.solvix.backend.domain.kb.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class KbArticleService {
    private static final int MAX_PAGE_SIZE = 50;

    private final KbArticleRepository articleRepository;

    public KbArticleService(KbArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional
    public KbArticle createArticle(String title, String content, String author, String category, Set<String> tags) {
        KbArticle article = new KbArticle(UUID.randomUUID(), title, content, author, category, tags);
        return articleRepository.save(article);
    }

    @Transactional
    public KbArticle findByIdAndIncrementView(UUID id) {
        KbArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found with ID: " + id));
        article.incrementViewCount();
        return articleRepository.save(article);
    }

    public KbArticle findById(UUID id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found with ID: " + id));
    }

    @Transactional
    public KbArticle publishArticle(UUID id) {
        KbArticle article = findById(id);
        article.publish();
        return articleRepository.save(article);
    }

    @Transactional
    public KbArticle archiveArticle(UUID id) {
        KbArticle article = findById(id);
        article.archive();
        return articleRepository.save(article);
    }

    public Page<KbArticle> searchArticles(Pageable pageable, String search, ArticleStatus status, String category, String tag) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable cappedPageable = PageRequest.of(pageable.getPageNumber(), pageSize, sort);

        return articleRepository.findPaged(cappedPageable, search, status, category, tag);
    }
}

package com.solvix.backend.web.kb;

import com.solvix.backend.application.kb.KbArticleService;
import com.solvix.backend.domain.kb.ArticleStatus;
import com.solvix.backend.domain.kb.KbArticle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kb/articles")
public class KbArticleController {
    private final KbArticleService articleService;

    public KbArticleController(KbArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'DEVELOPER', 'TEAM_LEAD', 'MANAGER', 'ADMINISTRATOR')")
    public ArticleResponse createArticle(@Valid @RequestBody CreateArticleRequest request, Authentication authentication) {
        return ArticleResponse.from(articleService.createArticle(
                request.title(),
                request.content(),
                authentication.getName(),
                request.category(),
                request.tags()
        ));
    }

    @GetMapping
    public PageResponse<ArticleResponse> searchArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag
    ) {
        String[] sortParts = sort.split(",");
        String sortProperty = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
        Page<KbArticle> articlePage = articleService.searchArticles(pageable, search, status, category, tag);

        return new PageResponse<>(
                articlePage.getContent().stream().map(ArticleResponse::from).toList(),
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public ArticleResponse getArticleDetails(@PathVariable UUID id) {
        return ArticleResponse.from(articleService.findByIdAndIncrementView(id));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'DEVELOPER', 'TEAM_LEAD', 'MANAGER', 'ADMINISTRATOR')")
    public ArticleResponse publishArticle(@PathVariable UUID id) {
        return ArticleResponse.from(articleService.publishArticle(id));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMINISTRATOR')")
    public ArticleResponse archiveArticle(@PathVariable UUID id) {
        return ArticleResponse.from(articleService.archiveArticle(id));
    }

    public record CreateArticleRequest(
            @NotBlank String title,
            @NotBlank String content,
            String category,
            Set<String> tags
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record ArticleResponse(
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
        public static ArticleResponse from(KbArticle article) {
            return new ArticleResponse(
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
    }
}

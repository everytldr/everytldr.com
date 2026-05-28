package org.everytldr.api.article;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.everytldr.api.support.language.ResolvedLanguage;
import org.everytldr.api.support.pagination.Pagination;
import org.everytldr.common.domain.article.ArticleRepository.DetailProjection;
import org.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import org.everytldr.common.domain.language.SupportedLanguage;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@Profile("api")
@Tag(name = "Articles")
public class ArticleController {
  private final ArticleService articleService;

  public ArticleController(ArticleService articleService) {
    this.articleService = articleService;
  }

  @GetMapping("/{id}")
  @Operation(operationId = "getArticle")
  public ArticleDetailResponse get(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @PathVariable Long id) {
    DetailProjection detail = articleService.getArticleDetail(id, language);
    return ArticleDetailResponse.from(detail);
  }

  @GetMapping
  @Operation(operationId = "listArticles")
  public ArticleListResponse list(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) String categoryPrefix,
      @RequestParam(required = false) Integer size) {
    int pageSize = Pagination.clampSize(size);
    ArticleListCursor.Decoded decoded = cursor == null ? null : ArticleListCursor.decode(cursor);

    Pagination.Page<ListItemProjection> page =
        articleService.listRecent(
            language,
            categoryPrefix,
            decoded == null ? null : decoded.publishedAt(),
            decoded == null ? null : decoded.id(),
            pageSize);

    List<ArticleListResponse.Item> items =
        page.items().stream().map(ArticleListResponse.Item::from).toList();
    String nextCursor =
        page.nextStart() == null
            ? null
            : ArticleListCursor.encode(page.nextStart().publishedAt(), page.nextStart().id());
    return new ArticleListResponse(items, nextCursor);
  }

  public record ArticleDetailResponse(
      Long id,
      String title,
      String summary,
      String category,
      Instant publishedAt,
      String source,
      String sourceUrl,
      String thumbnailUrl,
      long likeCount,
      long commentCount) {
    public static ArticleDetailResponse from(DetailProjection article) {
      return new ArticleDetailResponse(
          article.id(),
          article.title(),
          article.summary(),
          article.category(),
          article.publishedAt(),
          article.source(),
          article.sourceUrl(),
          article.thumbnailUrl(),
          article.likeCount(),
          article.commentCount());
    }
  }

  public record ArticleListResponse(List<Item> items, String nextCursor) {
    @Schema(name = "ArticleListItem")
    public record Item(
        @Schema(requiredMode = RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String summary,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"}) // TODO: thumbnailUrl 나중에 Nullable 제거해야함
            String thumbnailUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) String source,
        @Schema(requiredMode = RequiredMode.REQUIRED) String category) {
      static Item from(ListItemProjection projection) {
        return new Item(
            projection.id().toString(),
            projection.title(),
            projection.summary(),
            projection.thumbnailUrl(),
            projection.publishedAt(),
            projection.source(),
            projection.categorySlug());
      }
    }
  }
}

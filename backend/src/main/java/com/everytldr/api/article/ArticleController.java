package com.everytldr.api.article;

import com.everytldr.api.article.view.ArticleViewService;
import com.everytldr.api.support.language.ResolvedLanguage;
import com.everytldr.api.support.pagination.Pagination;
import com.everytldr.api.support.visitor.AnonymousVisitor;
import com.everytldr.api.support.visitor.ResolvedAnonymousVisitor;
import com.everytldr.common.domain.article.ArticleRepository.DetailProjection;
import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/articles")
@Profile("api")
@Tag(name = "Articles")
@RequiredArgsConstructor
public class ArticleController {
  private static final int MIN_QUERY_LENGTH = 2;
  private static final int DEFAULT_POPULAR_SIZE = 10;
  private static final int DEFAULT_RELATED_SIZE = 10;

  private final ArticleService articleService;
  private final ArticlePopularityService articlePopularityService;
  private final ArticleViewService articleViewService;
  private final LicensePolicyEvaluator licensePolicyEvaluator;

  @GetMapping("/{id}")
  @Operation(operationId = "getArticle")
  public ArticleDetailResponse get(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @PathVariable @Schema(type = "string") Long id) {
    DetailProjection detail = articleService.getArticleDetail(id, language);
    long viewCount = articleViewService.getViewCount(id, detail.viewCount());
    return ArticleDetailResponse.from(detail, licensePolicyEvaluator, viewCount);
  }

  @PostMapping("/{id}/views")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "countArticleView")
  public void countView(
      @PathVariable @Schema(type = "string") Long id,
      @Parameter(hidden = true) @ResolvedAnonymousVisitor AnonymousVisitor visitor) {
    articleViewService.recordView(id, visitor.visitorHash());
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
        page.items().stream()
            .map(item -> ArticleListResponse.Item.from(item, licensePolicyEvaluator))
            .toList();
    String nextCursor =
        page.nextStart() == null
            ? null
            : ArticleListCursor.encode(page.nextStart().publishedAt(), page.nextStart().id());
    return new ArticleListResponse(items, nextCursor);
  }

  @GetMapping("/popular")
  @Operation(operationId = "listPopularArticles")
  public ArticlePopularResponse listPopular(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @RequestParam(required = false) Integer size) {
    int pageSize = Pagination.clampSize(size == null ? DEFAULT_POPULAR_SIZE : size);
    List<ListItemProjection> popularArticles =
        articlePopularityService.listPopular(language, pageSize);
    List<ArticleListResponse.Item> items =
        popularArticles.stream()
            .map(item -> ArticleListResponse.Item.from(item, licensePolicyEvaluator))
            .toList();
    return new ArticlePopularResponse(items);
  }

  @GetMapping("/{id}/related")
  @Operation(operationId = "listRelatedArticles")
  public ArticleRelatedResponse listRelated(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @PathVariable @Schema(type = "string") Long id,
      @RequestParam(required = false) Integer size) {
    int pageSize = Pagination.clampSize(size == null ? DEFAULT_RELATED_SIZE : size);
    List<ListItemProjection> relatedArticles = articleService.listRelated(language, id, pageSize);
    List<ArticleListResponse.Item> items =
        relatedArticles.stream()
            .map(item -> ArticleListResponse.Item.from(item, licensePolicyEvaluator))
            .toList();
    return new ArticleRelatedResponse(items);
  }

  @GetMapping("/search")
  @Operation(operationId = "searchArticles")
  public ArticleSearchResponse search(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @RequestParam String q,
      @RequestParam(required = false) Integer offset,
      @RequestParam(required = false) Integer size) {
    if (q.trim().length() < MIN_QUERY_LENGTH) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "q must be at least " + MIN_QUERY_LENGTH + " characters");
    }
    int pageSize = Pagination.clampSize(size);
    int startOffset = offset == null ? 0 : Math.max(0, offset);

    ArticleService.SearchResult result = articleService.search(language, q, startOffset, pageSize);
    return ArticleSearchResponse.from(result, licensePolicyEvaluator);
  }

  public record ArticleDetailResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) String id,
      @Schema(requiredMode = RequiredMode.REQUIRED) String title,
      @Schema(requiredMode = RequiredMode.REQUIRED) String summary,
      @Schema(requiredMode = RequiredMode.REQUIRED) String category,
      @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt,
      @Schema(requiredMode = RequiredMode.REQUIRED) String source,
      @Schema(requiredMode = RequiredMode.REQUIRED) String contentUrl,
      @Schema(requiredMode = RequiredMode.REQUIRED) String licenseCode,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"})
          String licenseVersion,
      @Schema(requiredMode = RequiredMode.REQUIRED) boolean advertisingAllowed,
      @Schema(requiredMode = RequiredMode.REQUIRED) boolean requiresAttribution,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"}) // TODO: thumbnailUrl 나중에 Nullable 제거해야함
          String thumbnailUrl,
      @Schema(requiredMode = RequiredMode.REQUIRED) long likeCount,
      @Schema(requiredMode = RequiredMode.REQUIRED) long commentCount,
      @Schema(requiredMode = RequiredMode.REQUIRED) long viewCount) {
    public static ArticleDetailResponse from(
        DetailProjection article, LicensePolicyEvaluator licensePolicyEvaluator, long viewCount) {
      return new ArticleDetailResponse(
          article.id().toString(),
          article.title(),
          article.summary(),
          article.category(),
          article.publishedAt(),
          article.source(),
          article.contentUrl(),
          article.licenseCodeValue(),
          article.licenseVersion(),
          licensePolicyEvaluator.canDisplayAdvertising(article.licenseInfo()),
          licensePolicyEvaluator.requiresAttribution(article.licenseInfo()),
          article.thumbnailUrl(),
          article.likeCount(),
          article.commentCount(),
          viewCount);
    }
  }

  public record ArticleListResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<Item> items,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"})
          String nextCursor) {
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
        @Schema(requiredMode = RequiredMode.REQUIRED) String licenseCode,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            String licenseVersion,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean advertisingAllowed,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean requiresAttribution,
        @Schema(requiredMode = RequiredMode.REQUIRED) String category) {
      static Item from(
          ListItemProjection projection, LicensePolicyEvaluator licensePolicyEvaluator) {
        return new Item(
            projection.id().toString(),
            projection.title(),
            projection.summary(),
            projection.thumbnailUrl(),
            projection.publishedAt(),
            projection.source(),
            projection.licenseCodeValue(),
            projection.licenseVersion(),
            licensePolicyEvaluator.canDisplayAdvertising(projection.licenseInfo()),
            licensePolicyEvaluator.requiresAttribution(projection.licenseInfo()),
            projection.categorySlug());
      }
    }
  }

  public record ArticleSearchResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<ArticleListResponse.Item> items,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"integer", "null"})
          Integer nextOffset) {
    static ArticleSearchResponse from(
        ArticleService.SearchResult result, LicensePolicyEvaluator licensePolicyEvaluator) {
      List<ArticleListResponse.Item> items =
          result.items().stream()
              .map(item -> ArticleListResponse.Item.from(item, licensePolicyEvaluator))
              .toList();
      return new ArticleSearchResponse(items, result.nextOffset());
    }
  }

  public record ArticlePopularResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<ArticleListResponse.Item> items) {}

  public record ArticleRelatedResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<ArticleListResponse.Item> items) {}
}

package com.everytldr.api.article;

import com.everytldr.api.support.language.ResolvedLanguage;
import com.everytldr.api.support.pagination.Pagination;
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
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/articles")
@Profile("api")
@Tag(name = "Articles")
public class ArticleController {
  private final ArticleService articleService;
  private final LicensePolicyEvaluator licensePolicyEvaluator;

  public ArticleController(
      ArticleService articleService, LicensePolicyEvaluator licensePolicyEvaluator) {
    this.articleService = articleService;
    this.licensePolicyEvaluator = licensePolicyEvaluator;
  }

  @GetMapping("/{id}")
  @Operation(operationId = "getArticle")
  public ArticleDetailResponse get(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @PathVariable @Schema(type = "string") Long id) {
    DetailProjection detail = articleService.getArticleDetail(id, language);
    return ArticleDetailResponse.from(detail, licensePolicyEvaluator);
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

  @GetMapping("/search")
  @Operation(operationId = "searchArticles")
  public ArticleSearchResponse search(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @RequestParam String q,
      @RequestParam(required = false) Integer offset,
      @RequestParam(required = false) Integer size) {
    if (q.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must not be blank");
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
      @Schema(requiredMode = RequiredMode.REQUIRED) long commentCount) {
    public static ArticleDetailResponse from(
        DetailProjection article, LicensePolicyEvaluator licensePolicyEvaluator) {
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
          article.commentCount());
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
}

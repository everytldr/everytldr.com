package com.everytldr.api.sitemap;

import com.everytldr.api.sitemap.SitemapService.SitemapArticle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/sitemap")
@Profile("api")
@Tag(name = "Sitemap")
@RequiredArgsConstructor
public class SitemapController {
  private static final int DEFAULT_SIZE = 2000;
  private static final int MAX_SIZE = 5000;

  private final SitemapService sitemapService;

  @GetMapping("/articles")
  @Operation(operationId = "listSitemapArticles")
  public SitemapArticleListResponse listArticles(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    int pageNumber = assertValidPage(page);
    int pageSize = assertValidSize(size);

    long total = sitemapService.countArticles();
    List<SitemapArticle> articles = sitemapService.findArticles(pageNumber, pageSize);

    return SitemapArticleListResponse.from(articles, total);
  }

  private static int assertValidPage(Integer requested) {
    if (requested == null) {
      return 0;
    }
    if (requested < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must not be negative");
    }

    return requested;
  }

  private static int assertValidSize(Integer requested) {
    if (requested == null) {
      return DEFAULT_SIZE;
    }
    if (requested < 1 || requested > MAX_SIZE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "size must be between 1 and %d".formatted(MAX_SIZE));
    }

    return requested;
  }

  public record SitemapArticleListResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<Item> items,
      @Schema(requiredMode = RequiredMode.REQUIRED) long total) {
    public static SitemapArticleListResponse from(List<SitemapArticle> articles, long total) {
      List<Item> items = articles.stream().map(Item::from).toList();
      return new SitemapArticleListResponse(items, total);
    }

    @Schema(name = "SitemapArticleListItem")
    public record Item(
        @Schema(requiredMode = RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<String> languages) {
      public static Item from(SitemapArticle article) {
        return new Item(article.id().toString(), article.publishedAt(), article.languages());
      }
    }
  }
}

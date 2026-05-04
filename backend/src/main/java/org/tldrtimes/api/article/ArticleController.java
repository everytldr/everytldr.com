package org.tldrtimes.api.article;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tldrtimes.api.support.language.ResolvedLanguage;
import org.tldrtimes.api.support.pagination.Pagination;
import org.tldrtimes.common.domain.article.ArticleListProjection;
import org.tldrtimes.common.domain.language.SupportedLanguage;

@RestController
@RequestMapping("/api/articles")
@Profile("api")
public class ArticleController {
  private final ArticleListService articleListService;

  public ArticleController(ArticleListService articleListService) {
    this.articleListService = articleListService;
  }

  @GetMapping
  public ArticleListResponse list(
      @ResolvedLanguage SupportedLanguage language,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Integer size) {
    int pageSize = Pagination.clampSize(size);
    ArticleListCursor.Decoded decoded = cursor == null ? null : ArticleListCursor.decode(cursor);

    Pagination.Page<ArticleListProjection> page =
        articleListService.listRecent(
            language,
            category,
            decoded == null ? null : decoded.publishedAt(),
            decoded == null ? null : decoded.id(),
            pageSize);

    List<ArticleListItem> items = page.items().stream().map(ArticleListItem::from).toList();
    String nextCursor =
        page.nextStart() == null
            ? null
            : ArticleListCursor.encode(page.nextStart().publishedAt(), page.nextStart().id());
    return new ArticleListResponse(items, nextCursor);
  }
}

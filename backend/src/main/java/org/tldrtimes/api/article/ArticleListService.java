package org.tldrtimes.api.article;

import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.api.support.pagination.Pagination;
import org.tldrtimes.common.domain.article.ArticleListProjection;
import org.tldrtimes.common.domain.article.ArticleRepository;
import org.tldrtimes.common.domain.language.SupportedLanguage;

@Service
@Profile("api")
public class ArticleListService {
  private final ArticleRepository articleRepository;

  public ArticleListService(ArticleRepository articleRepository) {
    this.articleRepository = articleRepository;
  }

  @Transactional(readOnly = true)
  public Pagination.Page<ArticleListProjection> listRecent(
      SupportedLanguage language,
      String categorySlug,
      Instant cursorPublishedAt,
      Long cursorId,
      int size) {
    List<ArticleListProjection> rows =
        articleRepository.findRecent(
            language.code(),
            categorySlug,
            cursorPublishedAt,
            cursorId,
            PageRequest.of(
                0,
                size + 1, // NOTE: 행 하나를 추가로 받아와서 hasMore를 계산해냄.
                Sort.unsorted()));
    return Pagination.Page.from(rows, size);
  }
}

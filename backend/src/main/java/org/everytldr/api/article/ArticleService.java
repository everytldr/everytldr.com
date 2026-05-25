package org.everytldr.api.article;

import java.time.Instant;
import java.util.List;
import org.everytldr.api.support.pagination.Pagination;
import org.everytldr.common.domain.article.ArticleListProjection;
import org.everytldr.common.domain.article.ArticleRepository;
import org.everytldr.common.domain.language.SupportedLanguage;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("api")
public class ArticleService {
  private final ArticleRepository articleRepository;

  public ArticleService(ArticleRepository articleRepository) {
    this.articleRepository = articleRepository;
  }

  @Transactional(readOnly = true)
  public Pagination.Page<ArticleListProjection> listRecent(
      SupportedLanguage language,
      String categoryPrefix,
      Instant cursorPublishedAt,
      Long cursorId,
      int size) {
    List<ArticleListProjection> rows =
        articleRepository.findRecent(
            language.code(),
            categoryPrefix,
            cursorPublishedAt,
            cursorId,
            PageRequest.of(
                0,
                size + 1, // NOTE: 행 하나를 추가로 받아와서 hasMore를 계산해냄.
                Sort.unsorted()));
    return Pagination.Page.from(rows, size);
  }
}

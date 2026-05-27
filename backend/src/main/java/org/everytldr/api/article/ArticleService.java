package org.everytldr.api.article;

import java.time.Instant;
import java.util.List;
import org.everytldr.api.support.pagination.Pagination;
import org.everytldr.common.domain.article.Article;
import org.everytldr.common.domain.article.ArticleRepository;
import org.everytldr.common.domain.article.ArticleRepository.DetailProjection;
import org.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import org.everytldr.common.domain.language.SupportedLanguage;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Profile("api")
public class ArticleService {
  private final ArticleRepository articleRepository;

  public ArticleService(ArticleRepository articleRepository) {
    this.articleRepository = articleRepository;
  }

  public Article getArticleOrThrow(Long articleId) {
    return articleRepository
        .findById(articleId)
        .orElseThrow(() -> new ArticleExceptions.NotFound(articleId));
  }

  public void assertArticleExists(Long articleId) {
    if (!articleRepository.existsById(articleId)) {
      throw new ArticleExceptions.NotFound(articleId);
    }
  }

  public DetailProjection getArticleDetail(Long id, SupportedLanguage language) {
    return articleRepository
        .findDetailByIdAndLanguage(id, language.code())
        .orElseThrow(() -> new ArticleExceptions.NotFound(id));
  }

  public Pagination.Page<ListItemProjection> listRecent(
      SupportedLanguage language,
      String categoryPrefix,
      Instant cursorPublishedAt,
      Long cursorId,
      int size) {
    List<ListItemProjection> rows =
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

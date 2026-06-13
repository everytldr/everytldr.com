package com.everytldr.api.article;

import com.everytldr.api.support.pagination.Pagination;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleRepository.DetailProjection;
import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.language.SupportedLanguage;
import java.time.Instant;
import java.util.List;
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
    PageRequest pageRequest =
        PageRequest.of(
            0,
            size + 1, // NOTE: 행 하나를 추가로 받아와서 hasMore를 계산해냄.
            Sort.unsorted());
    List<ListItemProjection> rows =
        categoryPrefix == null
            ? articleRepository.findRecent(
                language.code(), cursorPublishedAt, cursorId, pageRequest)
            : articleRepository.findRecentByCategoryPrefix(
                language.code(), categoryPrefix, cursorPublishedAt, cursorId, pageRequest);
    return Pagination.Page.from(rows, size);
  }
}

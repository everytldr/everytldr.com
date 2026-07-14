package com.everytldr.api.article;

import com.everytldr.common.domain.article.Article;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("api")
@Slf4j
public class ArticleViewService {
  private final ArticleService articleService;
  private final ArticleViewRedisRepository articleViewRedisRepository;
  private final ArticleViewProperties properties;

  public void recordView(Long articleId, String visitorHash) {
    Objects.requireNonNull(articleId, "articleId must not be null");
    Objects.requireNonNull(visitorHash, "visitorHash must not be null");

    Article article = articleService.getArticleOrThrow(articleId);
    try {
      articleViewRedisRepository.recordViewIfUnique(
          articleId, visitorHash, article.getViewCount(), properties.deduplicationTtl());
    } catch (DataAccessException e) {
      throw new ArticleViewExceptions.Unavailable(e);
    }
  }

  public long getViewCount(Long articleId, long databaseViewCount) {
    Objects.requireNonNull(articleId, "articleId must not be null");
    try {
      return articleViewRedisRepository.findViewCount(articleId).orElse(databaseViewCount);
    } catch (DataAccessException e) {
      log.warn("Failed to read article view count from Redis. articleId={}", articleId, e);
      return databaseViewCount;
    }
  }
}

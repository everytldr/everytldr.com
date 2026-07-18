package com.everytldr.api.article;

import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.infrastructure.article.view.ArticleViewRedisRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
public class ArticlePopularityService {
  private static final int CANDIDATE_OVERFETCH_MULTIPLIER = 2;
  private static final int MAX_CANDIDATE_BATCH_SIZE = 100;

  private final ArticleService articleService;
  private final ArticleViewRedisRepository articleViewRedisRepository;
  private final ArticlePopularityProperties properties;
  private final Clock clock;

  public List<ListItemProjection> listPopular(SupportedLanguage language, int size) {
    Objects.requireNonNull(language, "language must not be null");
    List<Long> rankedArticleIds;
    try {
      rankedArticleIds =
          articleViewRedisRepository.findPopularArticleIds(
              Instant.now(clock), properties.bucketLookbackHours());
    } catch (DataAccessException e) {
      log.warn("Failed to read article popularity from Redis. Falling back to database.", e);
      return articleService.listMostViewed(language, size);
    }

    return findPublishableArticlesInRankOrder(language, rankedArticleIds, size);
  }

  private List<ListItemProjection> findPublishableArticlesInRankOrder(
      SupportedLanguage language, List<Long> rankedArticleIds, int size) {
    int candidateBatchSize =
        Math.min(size * CANDIDATE_OVERFETCH_MULTIPLIER, MAX_CANDIDATE_BATCH_SIZE);
    List<ListItemProjection> popularArticles = new java.util.ArrayList<>(size);
    for (int start = 0; start < rankedArticleIds.size() && popularArticles.size() < size; ) {
      int end = Math.min(start + candidateBatchSize, rankedArticleIds.size());
      int remainingSize = size - popularArticles.size();
      popularArticles.addAll(
          articleService.listByIdsInOrder(
              language, rankedArticleIds.subList(start, end), remainingSize));
      start = end;
    }
    return popularArticles;
  }
}

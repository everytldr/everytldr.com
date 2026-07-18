package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.infrastructure.article.view.ArticleViewRedisRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class ArticlePopularityServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-14T12:34:56Z");

  @Mock private ArticleService articleService;
  @Mock private ArticleViewRedisRepository redisRepository;

  private ArticlePopularityService service;

  @BeforeEach
  void setUp() {
    service =
        new ArticlePopularityService(
            articleService,
            redisRepository,
            new ArticlePopularityProperties(Duration.ofHours(26), 24),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void returnsArticlesInRedisRankOrder() {
    List<ListItemProjection> articles = List.of(listItem(3L), listItem(2L));
    when(redisRepository.findPopularArticleIds(NOW, 24)).thenReturn(List.of(3L, 2L));
    when(articleService.listByIdsInOrder(SupportedLanguage.KOREAN, List.of(3L, 2L), 10))
        .thenReturn(articles);

    List<ListItemProjection> result = service.listPopular(SupportedLanguage.KOREAN, 10);

    assertThat(result).containsExactlyElementsOf(articles);
  }

  @Test
  void fallsBackToDatabaseRankingWhenRedisIsUnavailable() {
    List<ListItemProjection> articles = List.of(listItem(3L), listItem(2L));
    when(redisRepository.findPopularArticleIds(NOW, 24))
        .thenThrow(new DataAccessResourceFailureException("redis unavailable"));
    when(articleService.listMostViewed(SupportedLanguage.KOREAN, 10)).thenReturn(articles);

    List<ListItemProjection> result = service.listPopular(SupportedLanguage.KOREAN, 10);

    assertThat(result).containsExactlyElementsOf(articles);
    verify(articleService).listMostViewed(SupportedLanguage.KOREAN, 10);
  }

  @Test
  void queriesRankedArticleIdsInBatchesUntilRequestedSizeIsFilled() {
    List<Long> rankedArticleIds = LongStream.rangeClosed(1, 200).boxed().toList();
    List<Long> firstBatch = rankedArticleIds.subList(0, 6);
    List<Long> secondBatch = rankedArticleIds.subList(6, 12);
    when(redisRepository.findPopularArticleIds(NOW, 24)).thenReturn(rankedArticleIds);
    when(articleService.listByIdsInOrder(SupportedLanguage.KOREAN, firstBatch, 3))
        .thenReturn(List.of(listItem(1L), listItem(2L)));
    when(articleService.listByIdsInOrder(SupportedLanguage.KOREAN, secondBatch, 1))
        .thenReturn(List.of(listItem(7L)));

    List<ListItemProjection> result = service.listPopular(SupportedLanguage.KOREAN, 3);

    assertThat(result).extracting(ListItemProjection::id).containsExactly(1L, 2L, 7L);
    verify(articleService).listByIdsInOrder(SupportedLanguage.KOREAN, firstBatch, 3);
    verify(articleService).listByIdsInOrder(SupportedLanguage.KOREAN, secondBatch, 1);
  }

  @Test
  void doesNotTreatDatabaseLookupFailureAsRedisFailure() {
    DataAccessResourceFailureException databaseFailure =
        new DataAccessResourceFailureException("database unavailable");
    when(redisRepository.findPopularArticleIds(NOW, 24)).thenReturn(List.of(3L));
    when(articleService.listByIdsInOrder(SupportedLanguage.KOREAN, List.of(3L), 10))
        .thenThrow(databaseFailure);

    assertThatThrownBy(() -> service.listPopular(SupportedLanguage.KOREAN, 10))
        .isSameAs(databaseFailure);

    verify(articleService, never()).listMostViewed(SupportedLanguage.KOREAN, 10);
  }

  private static ListItemProjection listItem(Long id) {
    return new ListItemProjection(
        id, "Title", "Summary", null, NOW, "Example", LicenseCode.CC_BY, "4.0", "football");
  }
}

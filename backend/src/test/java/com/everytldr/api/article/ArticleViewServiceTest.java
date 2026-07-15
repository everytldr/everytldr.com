package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.article.Article;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class ArticleViewServiceTest {
  private static final Duration DEDUPLICATION_TTL = Duration.ofHours(24);
  private static final Duration POPULARITY_BUCKET_TTL = Duration.ofHours(26);
  private static final Instant NOW = Instant.parse("2026-07-14T12:34:56Z");

  @Mock private ArticleService articleService;
  @Mock private ArticleViewRedisRepository redisRepository;

  private ArticleViewService service;

  @BeforeEach
  void setUp() {
    service =
        new ArticleViewService(
            articleService,
            redisRepository,
            new ArticleViewProperties(DEDUPLICATION_TTL, "0 0 * * * *"),
            new ArticlePopularityProperties(POPULARITY_BUCKET_TTL, 24),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void returnsRedisViewCountWhenPresent() {
    when(redisRepository.findViewCount(1L)).thenReturn(OptionalLong.of(12L));

    long result = service.getViewCount(1L, 10L);

    assertThat(result).isEqualTo(12L);
  }

  @Test
  void fallsBackToDatabaseCountWhenRedisKeyIsMissing() {
    when(redisRepository.findViewCount(1L)).thenReturn(OptionalLong.empty());

    long result = service.getViewCount(1L, 10L);

    assertThat(result).isEqualTo(10L);
  }

  @Test
  void fallsBackToDatabaseCountWhenRedisIsUnavailable() {
    when(redisRepository.findViewCount(1L))
        .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

    long result = service.getViewCount(1L, 10L);

    assertThat(result).isEqualTo(10L);
  }

  @Test
  void recordsViewUsingDatabaseCountAsRedisBaseline() {
    Article article = Article.create("https://example.com/a", "Example", null, "en", Instant.now());
    when(articleService.getArticleOrThrow(1L)).thenReturn(article);
    service.recordView(1L, "visitor-hash");

    verify(redisRepository)
        .recordViewIfUnique(1L, "visitor-hash", 0L, DEDUPLICATION_TTL, NOW, POPULARITY_BUCKET_TTL);
  }

  @Test
  void mapsRedisWriteFailureToUnavailableException() {
    Article article = Article.create("https://example.com/a", "Example", null, "en", Instant.now());
    when(articleService.getArticleOrThrow(1L)).thenReturn(article);
    doThrow(new DataAccessResourceFailureException("redis unavailable"))
        .when(redisRepository)
        .recordViewIfUnique(1L, "visitor-hash", 0L, DEDUPLICATION_TTL, NOW, POPULARITY_BUCKET_TTL);

    assertThatThrownBy(() -> service.recordView(1L, "visitor-hash"))
        .isInstanceOf(ArticleViewExceptions.Unavailable.class);
  }
}

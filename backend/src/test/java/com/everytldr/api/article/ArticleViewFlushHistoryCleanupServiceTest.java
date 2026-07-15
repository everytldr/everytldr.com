package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleViewFlushHistoryCleanupServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");

  @Mock private ArticleViewFlushRepository flushRepository;
  @Mock private ArticleViewRedisRepository redisRepository;

  @Test
  void repeatsDeletionUntilBatchIsNotFull() {
    ArticleViewFlushHistoryCleanupProperties properties =
        new ArticleViewFlushHistoryCleanupProperties(Duration.ofDays(30), Duration.ofDays(1), 2);
    ArticleViewFlushHistoryCleanupService cleanupService =
        new ArticleViewFlushHistoryCleanupService(
            flushRepository, redisRepository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    Instant cutoff = NOW.minus(Duration.ofDays(30));
    when(redisRepository.findFlushingBatchIds()).thenReturn(List.of("batch-1"));
    when(flushRepository.deleteHistoryBeforeExcluding(cutoff, List.of("batch-1"), 2))
        .thenReturn(2, 2, 1);

    cleanupService.deleteExpiredHistory();

    verify(flushRepository, times(3)).deleteHistoryBeforeExcluding(cutoff, List.of("batch-1"), 2);
  }

  @Test
  void keepsHistoryWhenRedisBatchLookupFails() {
    ArticleViewFlushHistoryCleanupProperties properties =
        new ArticleViewFlushHistoryCleanupProperties(Duration.ofDays(30), Duration.ofDays(1), 2);
    ArticleViewFlushHistoryCleanupService cleanupService =
        new ArticleViewFlushHistoryCleanupService(
            flushRepository, redisRepository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    when(redisRepository.findFlushingBatchIds())
        .thenThrow(new IllegalStateException("redis unavailable"));

    assertThatThrownBy(cleanupService::deleteExpiredHistory)
        .isInstanceOf(IllegalStateException.class);

    verify(flushRepository, never())
        .deleteHistoryBeforeExcluding(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyInt());
  }
}

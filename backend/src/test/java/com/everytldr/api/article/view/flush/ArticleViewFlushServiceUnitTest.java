package com.everytldr.api.article.view.flush;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.api.article.view.ArticleViewRedisRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleViewFlushServiceUnitTest {
  @Mock private ArticleViewRedisRepository redisRepository;
  @Mock private ArticleViewFlushWriter flushWriter;
  @InjectMocks private ArticleViewFlushService flushService;

  @Test
  void keepsFlushBatchWhenDatabaseWriteFails() {
    String key = "av:delta:flushing:batch-1";
    ArticleViewRedisRepository.FlushBatch batch =
        new ArticleViewRedisRepository.FlushBatch("batch-1", key, Map.of(1L, 3L));
    when(redisRepository.findFlushingKeys()).thenReturn(List.of(key), List.of());
    when(redisRepository.getFlushBatch(key)).thenReturn(batch);
    when(flushWriter.apply(batch)).thenThrow(new IllegalStateException("database unavailable"));

    flushService.flushPendingViews();

    verify(redisRepository, never()).deleteFlushBatch(key);
  }

  @Test
  void flushesExistingBatchBeforeMovingActiveDelta() {
    String key = "av:delta:flushing:batch-1";
    ArticleViewRedisRepository.FlushBatch batch =
        new ArticleViewRedisRepository.FlushBatch("batch-1", key, Map.of(1L, 3L));
    when(redisRepository.findFlushingKeys()).thenReturn(List.of(key));
    when(redisRepository.getFlushBatch(key)).thenReturn(batch);
    doThrow(new IllegalStateException("redis unavailable"))
        .when(redisRepository)
        .moveActiveDeltaToFlushBatch();

    assertThatThrownBy(flushService::flushPendingViews).isInstanceOf(IllegalStateException.class);

    verify(flushWriter).apply(batch);
    verify(redisRepository).deleteFlushBatch(key);
  }

  @Test
  void retriesBatchDeletionWithoutApplyingDatabaseBatchTwice() {
    String key = "av:delta:flushing:batch-1";
    ArticleViewRedisRepository.FlushBatch batch =
        new ArticleViewRedisRepository.FlushBatch("batch-1", key, Map.of(1L, 3L));
    when(redisRepository.findFlushingKeys())
        .thenReturn(List.of(key), List.of(), List.of(key), List.of());
    when(redisRepository.getFlushBatch(key)).thenReturn(batch);
    when(flushWriter.apply(batch))
        .thenReturn(
            ArticleViewFlushWriter.ApplyResult.APPLIED,
            ArticleViewFlushWriter.ApplyResult.ALREADY_APPLIED);
    doThrow(new IllegalStateException("redis unavailable"))
        .doNothing()
        .when(redisRepository)
        .deleteFlushBatch(key);

    flushService.flushPendingViews();
    flushService.flushPendingViews();

    verify(flushWriter, org.mockito.Mockito.times(2)).apply(batch);
    verify(redisRepository, org.mockito.Mockito.times(2)).deleteFlushBatch(key);
  }
}

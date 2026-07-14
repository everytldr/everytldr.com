package com.everytldr.api.article;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    when(redisRepository.findFlushingKeys()).thenReturn(List.of(key));
    when(redisRepository.getFlushBatch(key)).thenReturn(batch);
    when(flushWriter.apply(batch)).thenThrow(new IllegalStateException("database unavailable"));

    flushService.flushPendingViews();

    verify(redisRepository, never()).deleteFlushBatch(key);
  }
}

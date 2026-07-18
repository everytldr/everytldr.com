package com.everytldr.scheduler.article.view;

import com.everytldr.common.infrastructure.article.view.ArticleViewRedisRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("scheduler")
@Slf4j
public class ArticleViewFlushHistoryCleanupService {
  private final ArticleViewFlushRepository flushRepository;
  private final ArticleViewRedisRepository redisRepository;
  private final ArticleViewFlushHistoryCleanupProperties properties;
  private final Clock clock;

  public void deleteExpiredHistory() {
    Instant cutoff = clock.instant().minus(properties.retention());
    List<String> protectedBatchIds = redisRepository.findFlushingBatchIds();
    int deletedCount = 0;
    int deleted;
    do {
      deleted =
          flushRepository.deleteHistoryBeforeExcluding(
              cutoff, protectedBatchIds, properties.batchSize());
      deletedCount += deleted;
    } while (deleted == properties.batchSize());

    if (deletedCount > 0) {
      log.info("Deleted expired article view flush history. deletedCount={}", deletedCount);
    }
  }
}

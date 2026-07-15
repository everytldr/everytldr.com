package com.everytldr.api.article.view.flush;

import com.everytldr.api.article.view.ArticleViewRedisRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("api")
@Slf4j
public class ArticleViewFlushService {
  private final ArticleViewRedisRepository redisRepository;
  private final ArticleViewFlushWriter flushWriter;

  public void flushPendingViews() {
    flushExistingBatches();
    redisRepository.moveActiveDeltaToFlushBatch();
    flushExistingBatches();
  }

  private void flushExistingBatches() {
    List<String> flushingKeys = redisRepository.findFlushingKeys();
    for (String flushingKey : flushingKeys) {
      flushBatch(flushingKey);
    }
  }

  private void flushBatch(String flushingKey) {
    try {
      ArticleViewRedisRepository.FlushBatch batch = redisRepository.getFlushBatch(flushingKey);
      ArticleViewFlushWriter.ApplyResult result = flushWriter.apply(batch);
      redisRepository.deleteFlushBatch(batch.key());
      log.info(
          "Flushed article views. batchId={}, articleCount={}, result={}",
          batch.batchId(),
          batch.deltas().size(),
          result);
    } catch (RuntimeException e) {
      log.warn("Failed to flush article views. key={}", flushingKey, e);
    }
  }
}

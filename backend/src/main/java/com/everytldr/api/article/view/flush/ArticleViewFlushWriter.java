package com.everytldr.api.article.view.flush;

import com.everytldr.api.article.view.ArticleViewRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Profile("api")
public class ArticleViewFlushWriter {
  private final ArticleViewFlushRepository flushRepository;

  @Transactional
  public ApplyResult apply(ArticleViewRedisRepository.FlushBatch batch) {
    boolean newlyRegistered = flushRepository.registerBatch(batch.batchId());
    if (!newlyRegistered) {
      return ApplyResult.ALREADY_APPLIED;
    }
    flushRepository.incrementViewCounts(batch.deltas());
    return ApplyResult.APPLIED;
  }

  public enum ApplyResult {
    APPLIED,
    ALREADY_APPLIED
  }
}

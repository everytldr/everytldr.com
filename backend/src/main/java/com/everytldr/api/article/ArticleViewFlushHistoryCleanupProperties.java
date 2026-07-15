package com.everytldr.api.article;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.article-view.flush-history-cleanup")
public record ArticleViewFlushHistoryCleanupProperties(
    Duration retention, Duration interval, int batchSize) {
  public ArticleViewFlushHistoryCleanupProperties {
    assertPositive(retention, "retention");
    assertPositive(interval, "interval");
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
  }

  private static void assertPositive(Duration value, String name) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}

package com.everytldr.api.article;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.article-view.memory-guard")
public record ArticleViewMemoryGuardProperties(double threshold, Duration sampleInterval) {
  public ArticleViewMemoryGuardProperties {
    if (!Double.isFinite(threshold) || threshold <= 0 || threshold > 1) {
      throw new IllegalArgumentException("threshold must be greater than 0 and at most 1");
    }
    if (sampleInterval == null || sampleInterval.isZero() || sampleInterval.isNegative()) {
      throw new IllegalArgumentException("sampleInterval must be positive");
    }
  }
}

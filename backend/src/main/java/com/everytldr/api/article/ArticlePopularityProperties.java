package com.everytldr.api.article;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.article-popularity")
public record ArticlePopularityProperties(Duration bucketTtl, int bucketLookbackHours) {
  private static final int MAX_BUCKET_LOOKBACK_HOURS = 168;

  public ArticlePopularityProperties {
    Objects.requireNonNull(bucketTtl, "bucketTtl must not be null");
    if (bucketTtl.isZero() || bucketTtl.isNegative()) {
      throw new IllegalArgumentException("bucketTtl must be positive");
    }
    if (bucketTtl.toMillis() == 0) {
      throw new IllegalArgumentException("bucketTtl must be at least 1 millisecond");
    }
    if (bucketLookbackHours < 0) {
      throw new IllegalArgumentException("bucketLookbackHours must not be negative");
    }
    if (bucketLookbackHours > MAX_BUCKET_LOOKBACK_HOURS) {
      throw new IllegalArgumentException(
          "bucketLookbackHours must not exceed " + MAX_BUCKET_LOOKBACK_HOURS);
    }
    Duration minimumBucketTtl = Duration.ofHours((long) bucketLookbackHours + 1);
    if (bucketTtl.compareTo(minimumBucketTtl) < 0) {
      throw new IllegalArgumentException(
          "bucketTtl must be at least " + minimumBucketTtl + " for the configured lookback");
    }
  }
}

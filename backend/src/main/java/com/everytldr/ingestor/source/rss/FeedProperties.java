package com.everytldr.ingestor.source.rss;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.ingestor.ingestion.feed")
public record FeedProperties(Duration connectTimeout, Duration readTimeout) {

  public FeedProperties {
    if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
      throw new IllegalArgumentException("connectTimeout must be positive");
    }
    if (readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
      throw new IllegalArgumentException("readTimeout must be positive");
    }
  }
}

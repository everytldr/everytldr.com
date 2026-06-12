package com.everytldr.enricher.content;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.content")
public record ContentProperties(Duration timeout, int maxBodyBytes, int minBodyChars) {

  public ContentProperties {
    if (timeout == null || timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
    if (maxBodyBytes <= 0 || maxBodyBytes == Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "maxBodyBytes must be positive and below Integer.MAX_VALUE");
    }
    if (minBodyChars <= 0) {
      throw new IllegalArgumentException("minBodyChars must be positive");
    }
  }
}

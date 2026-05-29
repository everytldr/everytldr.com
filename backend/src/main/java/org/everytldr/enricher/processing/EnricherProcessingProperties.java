package org.everytldr.enricher.processing;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.processing")
public record EnricherProcessingProperties(
    boolean enabled,
    int batchSize,
    Duration fixedDelay,
    int maxAttempts,
    Duration retryDelay,
    Duration staleTimeout) {
  public EnricherProcessingProperties {
    if (batchSize < 1) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
    if (fixedDelay == null || fixedDelay.isNegative() || fixedDelay.isZero()) {
      throw new IllegalArgumentException("fixedDelay must be positive");
    }
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be positive");
    }
    if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()) {
      throw new IllegalArgumentException("retryDelay must be positive");
    }
    if (staleTimeout == null || staleTimeout.isNegative() || staleTimeout.isZero()) {
      throw new IllegalArgumentException("staleTimeout must be positive");
    }
  }
}

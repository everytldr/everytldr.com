package com.everytldr.enricher.processor;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.processing")
public record ProcessingProperties(
    boolean enabled,
    int batchSize,
    Duration fixedDelay,
    int maxAttempts,
    RetryProperties retry,
    Duration staleTimeout) {
  public ProcessingProperties {
    if (batchSize < 1) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
    if (fixedDelay == null || fixedDelay.isNegative() || fixedDelay.isZero()) {
      throw new IllegalArgumentException("fixedDelay must be positive");
    }
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be positive");
    }
    Objects.requireNonNull(retry, "retry must not be null");
    if (staleTimeout == null || staleTimeout.isNegative() || staleTimeout.isZero()) {
      throw new IllegalArgumentException("staleTimeout must be positive");
    }
  }

  public Duration calculateRetryDelay(int attemptCount) {
    if (attemptCount < 1) {
      throw new IllegalArgumentException("attemptCount must be positive");
    }

    long delayMillis = retry.initialInterval().toMillis();
    long maxDelayMillis = retry.maxInterval().toMillis();
    for (int attempt = 1; attempt < attemptCount && delayMillis < maxDelayMillis; attempt++) {
      delayMillis = Math.min((long) Math.ceil(delayMillis * retry.multiplier()), maxDelayMillis);
    }
    return Duration.ofMillis(delayMillis);
  }

  public record RetryProperties(Duration initialInterval, double multiplier, Duration maxInterval) {
    public RetryProperties {
      if (initialInterval == null || initialInterval.toMillis() < 1) {
        throw new IllegalArgumentException("retry.initialInterval must be at least 1ms");
      }
      if (!Double.isFinite(multiplier) || multiplier <= 1.0) {
        throw new IllegalArgumentException("retry.multiplier must be greater than 1.0");
      }
      if (maxInterval == null || maxInterval.toMillis() < 1) {
        throw new IllegalArgumentException("retry.maxInterval must be at least 1ms");
      }
      if (maxInterval.compareTo(initialInterval) < 0) {
        throw new IllegalArgumentException(
            "retry.maxInterval must not be shorter than retry.initialInterval");
      }
    }
  }
}

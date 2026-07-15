package com.everytldr.api.article.view;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("api")
@Slf4j
public class ArticleViewRedisMemoryGuard {
  private final ArticleViewRedisRepository redisRepository;
  private final ArticleViewMemoryGuardProperties properties;
  private final Clock clock;

  private volatile MemoryUsage memoryUsage = MemoryUsage.unavailable();
  private final AtomicBoolean memorySamplingFailed = new AtomicBoolean();

  @Scheduled(
      scheduler = "articleViewMemoryTaskScheduler",
      fixedDelayString = "${everytldr.article-view.memory-guard.sample-interval}")
  void refreshMemoryUsage() {
    try {
      ArticleViewRedisRepository.MemoryUsage redisMemoryUsage = redisRepository.findMemoryUsage();
      memoryUsage =
          MemoryUsage.available(
              redisMemoryUsage.usedBytes(), redisMemoryUsage.maxBytes(), clock.instant());
      if (memorySamplingFailed.compareAndSet(true, false)) {
        log.info("Redis memory usage sampling recovered");
      }
    } catch (RuntimeException e) {
      memoryUsage = memoryUsage.unavailableCopy();
      if (memorySamplingFailed.compareAndSet(false, true)) {
        log.warn("Failed to sample Redis memory usage", e);
      }
    }
  }

  public boolean hasReachedCapacity() {
    return getMemoryUsage().usageRatio() >= properties.threshold();
  }

  public MemoryUsage getMemoryUsage() {
    MemoryUsage currentMemoryUsage = memoryUsage;
    return currentMemoryUsage.isStaleAt(clock.instant(), properties.sampleInterval())
        ? currentMemoryUsage.unavailableCopy()
        : currentMemoryUsage;
  }

  public record MemoryUsage(
      long usedBytes, long maxBytes, boolean sampleAvailable, Instant sampledAt) {
    static MemoryUsage unavailable() {
      return new MemoryUsage(0, 0, false, Instant.EPOCH);
    }

    static MemoryUsage available(long usedBytes, long maxBytes, Instant sampledAt) {
      return new MemoryUsage(usedBytes, maxBytes, true, sampledAt);
    }

    MemoryUsage unavailableCopy() {
      return new MemoryUsage(usedBytes, maxBytes, false, sampledAt);
    }

    boolean isStaleAt(Instant currentTime, Duration sampleInterval) {
      return sampleAvailable
          && sampledAt.plus(sampleInterval.multipliedBy(2)).isBefore(currentTime);
    }

    public double usageRatio() {
      if (!sampleAvailable || maxBytes == 0) {
        return Double.NaN;
      }
      return (double) usedBytes / maxBytes;
    }
  }
}

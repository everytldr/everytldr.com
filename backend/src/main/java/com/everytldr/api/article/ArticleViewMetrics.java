package com.everytldr.api.article;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("api")
public class ArticleViewMetrics {
  private static final String REDIS_MEMORY_USED_METRIC =
      "everytldr.article.view.redis.memory.used.bytes";
  private static final String REDIS_MEMORY_MAX_METRIC =
      "everytldr.article.view.redis.memory.max.bytes";
  private static final String REDIS_MEMORY_USAGE_METRIC =
      "everytldr.article.view.redis.memory.usage";
  private static final String REDIS_MEMORY_SAMPLE_AVAILABLE_METRIC =
      "everytldr.article.view.redis.memory.sample.available";
  private static final String RECORD_REJECTED_METRIC = "everytldr.article.view.records.rejected";

  private final Counter capacityRejected;
  private final Counter redisErrorRejected;

  public ArticleViewMetrics(
      MeterRegistry meterRegistry, ArticleViewRedisMemoryGuard articleViewRedisMemoryGuard) {
    Gauge.builder(
            REDIS_MEMORY_USED_METRIC,
            articleViewRedisMemoryGuard,
            guard -> guard.getMemoryUsage().usedBytes())
        .register(meterRegistry);
    Gauge.builder(
            REDIS_MEMORY_MAX_METRIC,
            articleViewRedisMemoryGuard,
            guard -> guard.getMemoryUsage().maxBytes())
        .register(meterRegistry);
    Gauge.builder(
            REDIS_MEMORY_USAGE_METRIC,
            articleViewRedisMemoryGuard,
            guard -> guard.getMemoryUsage().usageRatio())
        .register(meterRegistry);
    Gauge.builder(
            REDIS_MEMORY_SAMPLE_AVAILABLE_METRIC,
            articleViewRedisMemoryGuard,
            guard -> guard.getMemoryUsage().sampleAvailable() ? 1 : 0)
        .register(meterRegistry);

    capacityRejected = meterRegistry.counter(RECORD_REJECTED_METRIC, "reason", "capacity");
    redisErrorRejected = meterRegistry.counter(RECORD_REJECTED_METRIC, "reason", "redis_error");
  }

  public void recordCapacityRejected() {
    capacityRejected.increment();
  }

  public void recordRedisErrorRejected() {
    redisErrorRejected.increment();
  }
}

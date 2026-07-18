package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.everytldr.common.infrastructure.article.view.ArticleViewRedisRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleViewMetricsTest {
  @Mock private ArticleViewRedisRepository redisRepository;

  @Test
  void exposesMemorySampleStateAndRejectedRecordCounters() {
    ArticleViewRedisMemoryGuard memoryGuard =
        new ArticleViewRedisMemoryGuard(
            redisRepository,
            new ArticleViewMemoryGuardProperties(0.9, Duration.ofSeconds(10)),
            Clock.systemUTC());
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleViewMetrics metrics = new ArticleViewMetrics(meterRegistry, memoryGuard);
    when(redisRepository.findMemoryUsage())
        .thenReturn(new ArticleViewRedisRepository.MemoryUsage(80, 100))
        .thenThrow(new IllegalStateException("Redis unavailable"));

    memoryGuard.refreshMemoryUsage();
    metrics.recordCapacityRejected();
    metrics.recordRedisErrorRejected();

    assertThat(meterRegistry.get("everytldr.article.view.redis.memory.usage").gauge().value())
        .isEqualTo(0.8);
    assertThat(
            meterRegistry
                .get("everytldr.article.view.redis.memory.sample.available")
                .gauge()
                .value())
        .isEqualTo(1);
    assertThat(
            meterRegistry
                .get("everytldr.article.view.records.rejected")
                .tag("reason", "capacity")
                .counter()
                .count())
        .isEqualTo(1);

    memoryGuard.refreshMemoryUsage();

    assertThat(meterRegistry.get("everytldr.article.view.redis.memory.usage").gauge().value())
        .isNaN();
    assertThat(
            meterRegistry
                .get("everytldr.article.view.redis.memory.sample.available")
                .gauge()
                .value())
        .isZero();
    assertThat(
            meterRegistry
                .get("everytldr.article.view.records.rejected")
                .tag("reason", "redis_error")
                .counter()
                .count())
        .isEqualTo(1);
  }
}

package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ArticleViewRedisMemoryGuardTest {
  @Mock private ArticleViewRedisRepository redisRepository;
  @Mock private Clock clock;

  private ArticleViewRedisMemoryGuard memoryGuard;
  private final Logger logger = (Logger) LoggerFactory.getLogger(ArticleViewRedisMemoryGuard.class);
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

  @BeforeEach
  void setUp() {
    memoryGuard =
        new ArticleViewRedisMemoryGuard(
            redisRepository,
            new ArticleViewMemoryGuardProperties(0.9, Duration.ofSeconds(10)),
            clock);
    when(clock.instant()).thenReturn(Instant.parse("2026-07-16T00:00:00Z"));
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(logAppender);
    logAppender.stop();
  }

  @Test
  void rejectsWritesWhenLatestSampleReachesThreshold() {
    when(redisRepository.findMemoryUsage())
        .thenReturn(new ArticleViewRedisRepository.MemoryUsage(90, 100));

    memoryGuard.refreshMemoryUsage();

    assertThat(memoryGuard.hasReachedCapacity()).isTrue();
    assertThat(memoryGuard.getMemoryUsage().sampleAvailable()).isTrue();
    assertThat(memoryGuard.getMemoryUsage().usageRatio()).isEqualTo(0.9);
  }

  @Test
  void preservesLastBytesAndMarksSampleUnavailableWhenSamplingFails() {
    when(redisRepository.findMemoryUsage())
        .thenReturn(new ArticleViewRedisRepository.MemoryUsage(80, 100))
        .thenThrow(new IllegalStateException("Redis unavailable"))
        .thenThrow(new IllegalStateException("Redis unavailable"));

    memoryGuard.refreshMemoryUsage();
    memoryGuard.refreshMemoryUsage();
    memoryGuard.refreshMemoryUsage();

    assertThat(memoryGuard.hasReachedCapacity()).isFalse();
    assertThat(memoryGuard.getMemoryUsage().usedBytes()).isEqualTo(80);
    assertThat(memoryGuard.getMemoryUsage().maxBytes()).isEqualTo(100);
    assertThat(memoryGuard.getMemoryUsage().sampleAvailable()).isFalse();
    assertThat(memoryGuard.getMemoryUsage().usageRatio()).isNaN();
  }

  @Test
  void logsOnlyMemorySamplingFailureAndRecoveryTransitions() {
    when(redisRepository.findMemoryUsage())
        .thenThrow(new IllegalStateException("Redis unavailable"))
        .thenThrow(new IllegalStateException("Redis unavailable"))
        .thenReturn(new ArticleViewRedisRepository.MemoryUsage(80, 100));

    memoryGuard.refreshMemoryUsage();
    memoryGuard.refreshMemoryUsage();
    memoryGuard.refreshMemoryUsage();

    List<ILoggingEvent> events = logAppender.list;
    assertThat(events)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Failed to sample Redis memory usage", Level.WARN),
            org.assertj.core.groups.Tuple.tuple(
                "Redis memory usage sampling recovered", Level.INFO));
    assertThat(events.getFirst().getThrowableProxy()).isNotNull();
  }

  @Test
  void doesNotRejectWritesFromAStaleSample() {
    Instant sampledAt = Instant.parse("2026-07-16T00:00:00Z");
    when(clock.instant()).thenReturn(sampledAt, sampledAt.plusSeconds(21));
    when(redisRepository.findMemoryUsage())
        .thenReturn(new ArticleViewRedisRepository.MemoryUsage(90, 100));

    memoryGuard.refreshMemoryUsage();

    assertThat(memoryGuard.hasReachedCapacity()).isFalse();
    assertThat(memoryGuard.getMemoryUsage().sampleAvailable()).isFalse();
    assertThat(memoryGuard.getMemoryUsage().usageRatio()).isNaN();
  }
}

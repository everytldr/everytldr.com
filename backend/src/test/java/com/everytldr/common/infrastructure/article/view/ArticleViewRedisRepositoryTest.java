package com.everytldr.common.infrastructure.article.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"api", "test"})
class ArticleViewRedisRepositoryTest {
  private static final Duration DEDUPLICATION_TTL = Duration.ofHours(24);
  private static final Duration POPULARITY_BUCKET_TTL = Duration.ofHours(26);
  private static final Instant VIEWED_AT = Instant.parse("2026-07-14T12:34:56Z");

  @Autowired private ArticleViewRedisRepository repository;
  @Autowired private StringRedisTemplate redisTemplate;

  @BeforeEach
  void clearRedis() {
    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              connection.serverCommands().flushDb();
              return null;
            });
  }

  @Test
  void countsFirstViewAndRejectsDuplicateVisitor() {
    long firstViewCount =
        repository.recordViewIfUnique(
            42L, "visitor-a", 7L, DEDUPLICATION_TTL, VIEWED_AT, POPULARITY_BUCKET_TTL);
    long duplicateViewCount =
        repository.recordViewIfUnique(
            42L, "visitor-a", 7L, DEDUPLICATION_TTL, VIEWED_AT, POPULARITY_BUCKET_TTL);
    long anotherVisitorViewCount =
        repository.recordViewIfUnique(
            42L, "visitor-b", 7L, DEDUPLICATION_TTL, VIEWED_AT, POPULARITY_BUCKET_TTL);

    assertThat(firstViewCount).isEqualTo(8L);
    assertThat(duplicateViewCount).isEqualTo(8L);
    assertThat(anotherVisitorViewCount).isEqualTo(9L);
    assertThat(repository.findViewCount(42L)).hasValue(9L);
    assertThat(redisTemplate.opsForHash().get("av:delta:active", "42")).isEqualTo("2");
    assertThat(redisTemplate.getExpire("av:seen:v1:42:visitor-a"))
        .isBetween(Duration.ofHours(23).toSeconds(), Duration.ofHours(24).toSeconds());
    assertThat(redisTemplate.opsForZSet().score("av:popular:v1:2026071412", "42")).isEqualTo(2.0);
    assertThat(redisTemplate.getExpire("av:popular:v1:2026071412"))
        .isBetween(Duration.ofHours(25).toSeconds(), Duration.ofHours(26).toSeconds());
  }

  @Test
  void findsRedisMemoryUsage() {
    ArticleViewRedisRepository.MemoryUsage memoryUsage = repository.findMemoryUsage();

    assertThat(memoryUsage.usedBytes()).isGreaterThanOrEqualTo(0);
    assertThat(memoryUsage.maxBytes()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void movesActiveDeltaWithoutCreatingCountSnapshot() {
    redisTemplate.opsForHash().put("av:delta:active", "42", "1");
    redisTemplate.opsForHash().put("av:count:v1:42", "unexpected", "value");

    repository.moveActiveDeltaToFlushBatch();

    assertThat(redisTemplate.opsForHash().get("av:delta:active", "42")).isNull();
    assertThat(repository.findFlushingKeys()).hasSize(1);
    assertThat(redisTemplate.keys("av:count:flushing:*")).isEmpty();
  }

  @Test
  void concurrentDuplicateRequestsCountOnlyOnce() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int index = 0; index < 20; index++) {
        futures.add(
            executor.submit(
                () ->
                    repository.recordViewIfUnique(
                        99L,
                        "same-visitor",
                        0L,
                        DEDUPLICATION_TTL,
                        VIEWED_AT,
                        POPULARITY_BUCKET_TTL)));
      }

      for (Future<?> future : futures) {
        future.get();
      }

      assertThat(repository.findViewCount(99L)).hasValue(1L);
      assertThat(redisTemplate.opsForHash().get("av:delta:active", "99")).isEqualTo("1");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void sumsHourlyPopularityBucketsInDescendingOrder() {
    repository.recordViewIfUnique(
        41L,
        "visitor-a",
        0L,
        DEDUPLICATION_TTL,
        VIEWED_AT.minus(Duration.ofHours(1)),
        POPULARITY_BUCKET_TTL);
    repository.recordViewIfUnique(
        41L, "visitor-b", 0L, DEDUPLICATION_TTL, VIEWED_AT, POPULARITY_BUCKET_TTL);
    repository.recordViewIfUnique(
        42L, "visitor-a", 0L, DEDUPLICATION_TTL, VIEWED_AT, POPULARITY_BUCKET_TTL);

    assertThat(repository.findPopularArticleIds(VIEWED_AT, 1)).containsExactly(41L, 42L);
  }

  @Test
  void rollsBackCountAndDeltaWhenSeenKeyCannotBeAllocated() {
    redisTemplate.opsForValue().set("av:count:v1:100", "10");
    redisTemplate.opsForHash().put("av:delta:active", "100", "5");
    String originalMaxMemory = findConfigValue("maxmemory");

    try {
      updateMaxMemory(findUsedMemoryBytes());

      assertThatThrownBy(
              () ->
                  repository.recordViewIfUnique(
                      100L,
                      "new-visitor",
                      10L,
                      DEDUPLICATION_TTL,
                      VIEWED_AT,
                      POPULARITY_BUCKET_TTL))
          .isInstanceOf(DataAccessException.class);

      assertThat(redisTemplate.opsForValue().get("av:count:v1:100")).isEqualTo("10");
      assertThat(redisTemplate.opsForHash().get("av:delta:active", "100")).isEqualTo("5");
      assertThat(redisTemplate.hasKey("av:seen:v1:100:new-visitor")).isFalse();
      assertThat(redisTemplate.opsForZSet().score("av:popular:v1:2026071412", "100")).isNull();
    } finally {
      updateMaxMemory(originalMaxMemory);
    }
  }

  @Test
  void rollsBackCountAndDeltaWhenPopularityBucketHasWrongType() {
    redisTemplate.opsForValue().set("av:count:v1:101", "10");
    redisTemplate.opsForHash().put("av:delta:active", "101", "5");
    redisTemplate.opsForValue().set("av:popular:v1:2026071412", "not-a-zset");

    assertThatThrownBy(
            () ->
                repository.recordViewIfUnique(
                    101L, "new-visitor", 10L, DEDUPLICATION_TTL, VIEWED_AT, POPULARITY_BUCKET_TTL))
        .isInstanceOf(DataAccessException.class);

    assertThat(redisTemplate.opsForValue().get("av:count:v1:101")).isEqualTo("10");
    assertThat(redisTemplate.opsForHash().get("av:delta:active", "101")).isEqualTo("5");
    assertThat(redisTemplate.hasKey("av:seen:v1:101:new-visitor")).isFalse();
    assertThat(redisTemplate.opsForValue().get("av:popular:v1:2026071412")).isEqualTo("not-a-zset");
  }

  private String findConfigValue(String name) {
    return redisTemplate.execute(
        (RedisCallback<String>)
            connection -> connection.serverCommands().getConfig(name).getProperty(name));
  }

  private String findUsedMemoryBytes() {
    return redisTemplate.execute(
        (RedisCallback<String>)
            connection -> {
              Properties memoryInfo = connection.serverCommands().info("memory");
              return memoryInfo.getProperty("used_memory");
            });
  }

  private void updateMaxMemory(String value) {
    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              connection.serverCommands().setConfig("maxmemory", value);
              return null;
            });
  }
}

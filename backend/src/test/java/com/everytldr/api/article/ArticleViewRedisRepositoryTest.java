package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import java.time.Duration;
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
    repository.recordViewIfUnique(42L, "visitor-a", 7L, DEDUPLICATION_TTL);
    repository.recordViewIfUnique(42L, "visitor-a", 7L, DEDUPLICATION_TTL);
    repository.recordViewIfUnique(42L, "visitor-b", 7L, DEDUPLICATION_TTL);

    assertThat(repository.findViewCount(42L)).hasValue(9L);
    assertThat(redisTemplate.opsForHash().get("av:delta:active", "42")).isEqualTo("2");
    assertThat(redisTemplate.getExpire("av:seen:v1:42:visitor-a"))
        .isBetween(Duration.ofHours(23).toSeconds(), Duration.ofHours(24).toSeconds());
  }

  @Test
  void concurrentDuplicateRequestsCountOnlyOnce() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int index = 0; index < 20; index++) {
        futures.add(
            executor.submit(
                () -> repository.recordViewIfUnique(99L, "same-visitor", 0L, DEDUPLICATION_TTL)));
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
  void rollsBackCountAndDeltaWhenSeenKeyCannotBeAllocated() {
    redisTemplate.opsForValue().set("av:count:v1:100", "10");
    redisTemplate.opsForHash().put("av:delta:active", "100", "5");
    String originalMaxMemory = findConfigValue("maxmemory");

    try {
      updateMaxMemory(findUsedMemoryBytes());

      assertThatThrownBy(
              () -> repository.recordViewIfUnique(100L, "new-visitor", 10L, DEDUPLICATION_TTL))
          .isInstanceOf(DataAccessException.class);

      assertThat(redisTemplate.opsForValue().get("av:count:v1:100")).isEqualTo("10");
      assertThat(redisTemplate.opsForHash().get("av:delta:active", "100")).isEqualTo("5");
      assertThat(redisTemplate.hasKey("av:seen:v1:100:new-visitor")).isFalse();
    } finally {
      updateMaxMemory(originalMaxMemory);
    }
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

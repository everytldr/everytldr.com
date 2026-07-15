package com.everytldr.api.article;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Profile("api")
public class ArticleViewRedisRepository {
  private static final String SEEN_KEY_PREFIX = "av:seen:v1:";
  private static final String COUNT_KEY_PREFIX = "av:count:v1:";
  private static final String ACTIVE_DELTA_KEY = "av:delta:active";
  private static final String FLUSHING_DELTA_KEY_PREFIX = "av:delta:flushing:";
  private static final String FLUSHING_DELTA_KEY_PATTERN = FLUSHING_DELTA_KEY_PREFIX + "*";
  private static final String POPULARITY_BUCKET_KEY_PREFIX = "av:popular:v1:";
  private static final DateTimeFormatter POPULARITY_BUCKET_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);

  private static final DefaultRedisScript<Long> COUNT_VIEW_SCRIPT = countViewScript();
  private static final DefaultRedisScript<Long> MOVE_DELTA_SCRIPT = moveDeltaScript();

  private final StringRedisTemplate redisTemplate;

  /** 하나의 Redis Lua script로 중복 검사, count 초기화, count/delta 증가와 실패 시 rollback을 원자적으로 처리한다. */
  public void recordViewIfUnique(
      Long articleId,
      String visitorHash,
      long databaseViewCount,
      Duration deduplicationTtl,
      Instant viewedAt,
      Duration popularityBucketTtl) {
    Objects.requireNonNull(articleId, "articleId must not be null");
    Objects.requireNonNull(visitorHash, "visitorHash must not be null");
    Objects.requireNonNull(deduplicationTtl, "deduplicationTtl must not be null");
    Objects.requireNonNull(viewedAt, "viewedAt must not be null");
    Objects.requireNonNull(popularityBucketTtl, "popularityBucketTtl must not be null");

    redisTemplate.execute(
        COUNT_VIEW_SCRIPT,
        List.of(
            createSeenKey(articleId, visitorHash),
            createCountKey(articleId),
            ACTIVE_DELTA_KEY,
            createPopularityBucketKey(viewedAt)),
        Long.toString(deduplicationTtl.toMillis()),
        Long.toString(databaseViewCount),
        articleId.toString(),
        Long.toString(popularityBucketTtl.toMillis()));
  }

  public OptionalLong findViewCount(Long articleId) {
    Objects.requireNonNull(articleId, "articleId must not be null");
    String value = redisTemplate.opsForValue().get(createCountKey(articleId));
    return value == null ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(value));
  }

  public List<Long> findPopularArticleIds(Instant currentTime, int bucketLookbackHours) {
    Objects.requireNonNull(currentTime, "currentTime must not be null");
    if (bucketLookbackHours < 0) {
      throw new IllegalArgumentException("bucketLookbackHours must not be negative");
    }

    Map<Long, Double> scoresByArticleId = new HashMap<>();
    for (int hoursAgo = 0; hoursAgo <= bucketLookbackHours; hoursAgo++) {
      Set<TypedTuple<String>> bucketScores =
          redisTemplate
              .opsForZSet()
              .reverseRangeWithScores(
                  createPopularityBucketKey(currentTime.minus(hoursAgo, ChronoUnit.HOURS)), 0, -1);
      if (bucketScores == null) {
        continue;
      }
      for (TypedTuple<String> bucketScore : bucketScores) {
        scoresByArticleId.merge(
            Long.parseLong(bucketScore.getValue()), bucketScore.getScore(), Double::sum);
      }
    }

    return scoresByArticleId.entrySet().stream()
        .sorted(
            (left, right) -> {
              int scoreComparison = Double.compare(right.getValue(), left.getValue());
              return scoreComparison != 0
                  ? scoreComparison
                  : Long.compare(right.getKey(), left.getKey());
            })
        .map(Map.Entry::getKey)
        .toList();
  }

  /** Lua RENAME으로 active delta를 flushing batch로 원자 이동해 DB flush 중 새 증가량이 기존 batch에 섞이지 않도록 한다. */
  public void moveActiveDeltaToFlushBatch() {
    String batchId = UUID.randomUUID().toString();
    String flushingKey = createFlushingKey(batchId);
    redisTemplate.execute(MOVE_DELTA_SCRIPT, List.of(ACTIVE_DELTA_KEY, flushingKey));
  }

  public List<String> findFlushingKeys() {
    List<String> keys = new ArrayList<>();
    ScanOptions options =
        ScanOptions.scanOptions().match(FLUSHING_DELTA_KEY_PATTERN).count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      cursor.forEachRemaining(keys::add);
    }
    return keys;
  }

  public FlushBatch getFlushBatch(String key) {
    Objects.requireNonNull(key, "key must not be null");
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
    Map<Long, Long> deltas = new LinkedHashMap<>();
    entries.forEach(
        (articleId, delta) ->
            deltas.put(Long.parseLong(articleId.toString()), Long.parseLong(delta.toString())));
    return new FlushBatch(extractBatchId(key), key, Map.copyOf(deltas));
  }

  public void deleteFlushBatch(String key) {
    redisTemplate.delete(key);
  }

  private static String createSeenKey(Long articleId, String visitorHash) {
    return SEEN_KEY_PREFIX + articleId + ":" + visitorHash;
  }

  private static String createCountKey(Long articleId) {
    return COUNT_KEY_PREFIX + articleId;
  }

  private static String createFlushingKey(String batchId) {
    return FLUSHING_DELTA_KEY_PREFIX + batchId;
  }

  private static String createPopularityBucketKey(Instant time) {
    return POPULARITY_BUCKET_KEY_PREFIX + POPULARITY_BUCKET_FORMATTER.format(time);
  }

  private static String extractBatchId(String key) {
    if (!key.startsWith(FLUSHING_DELTA_KEY_PREFIX)) {
      throw new IllegalArgumentException("Invalid article view flush key: " + key);
    }
    return key.substring(FLUSHING_DELTA_KEY_PREFIX.length());
  }

  private static DefaultRedisScript<Long> countViewScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setResultType(Long.class);
    script.setScriptText(
        """
        local function isError(result)
          return type(result) == 'table' and result.err ~= nil
        end

        if redis.call('EXISTS', KEYS[1]) == 1 then
          return 0
        end

        local countInitialized = redis.pcall('SETNX', KEYS[2], ARGV[2])
        if isError(countInitialized) then
          return redis.error_reply(countInitialized.err)
        end

        local function removeInitializedCount()
          if countInitialized == 1 then
            redis.pcall('DEL', KEYS[2])
          end
        end

        local function rollbackDelta()
          local restored = redis.pcall('HINCRBY', KEYS[3], ARGV[3], -1)
          if type(restored) == 'number' and restored == 0 then
            redis.pcall('HDEL', KEYS[3], ARGV[3])
          end
        end

        local function rollbackPopularity()
          local restored = redis.pcall('ZINCRBY', KEYS[4], -1, ARGV[3])
          if tonumber(restored) == 0 then
            redis.pcall('ZREM', KEYS[4], ARGV[3])
          end
        end

        local deltaIncremented = redis.pcall('HINCRBY', KEYS[3], ARGV[3], 1)
        if isError(deltaIncremented) then
          removeInitializedCount()
          return redis.error_reply(deltaIncremented.err)
        end

        local countIncremented = redis.pcall('INCR', KEYS[2])
        if isError(countIncremented) then
          rollbackDelta()
          removeInitializedCount()
          return redis.error_reply(countIncremented.err)
        end

        local popularityIncremented = redis.pcall('ZINCRBY', KEYS[4], 1, ARGV[3])
        if isError(popularityIncremented) then
          redis.pcall('DECR', KEYS[2])
          rollbackDelta()
          removeInitializedCount()
          return redis.error_reply(popularityIncremented.err)
        end

        local popularityExpirySet = redis.pcall('PEXPIRE', KEYS[4], ARGV[4])
        if isError(popularityExpirySet) then
          rollbackPopularity()
          redis.pcall('DECR', KEYS[2])
          rollbackDelta()
          removeInitializedCount()
          return redis.error_reply(popularityExpirySet.err)
        end

        local firstView = redis.pcall('SET', KEYS[1], '1', 'NX', 'PX', ARGV[1])
        if isError(firstView) then
          rollbackPopularity()
          redis.pcall('DECR', KEYS[2])
          rollbackDelta()
          removeInitializedCount()
          return redis.error_reply(firstView.err)
        end
        if not firstView then
          rollbackPopularity()
          redis.pcall('DECR', KEYS[2])
          rollbackDelta()
          removeInitializedCount()
          return 0
        end

        return 1
        """);
    return script;
  }

  private static DefaultRedisScript<Long> moveDeltaScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setResultType(Long.class);
    script.setScriptText(
        """
        if redis.call('EXISTS', KEYS[1]) == 0 then
          return 0
        end
        redis.call('RENAME', KEYS[1], KEYS[2])
        return 1
        """);
    return script;
  }

  public record FlushBatch(String batchId, String key, Map<Long, Long> deltas) {}
}

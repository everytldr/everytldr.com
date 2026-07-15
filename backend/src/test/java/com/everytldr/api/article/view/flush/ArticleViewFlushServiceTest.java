package com.everytldr.api.article.view.flush;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.api.article.view.ArticleViewRedisRepository;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"api", "test"})
class ArticleViewFlushServiceTest {
  private static final Duration DEDUPLICATION_TTL = Duration.ofHours(24);

  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSourceRepository sourceRepository;
  @Autowired private ArticleViewRedisRepository redisRepository;
  @Autowired private ArticleViewFlushService flushService;
  @Autowired private ArticleViewFlushWriter flushWriter;
  @Autowired private ArticleViewFlushRepository flushRepository;
  @Autowired private StringRedisTemplate redisTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

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
  void flushesDeltaToDatabaseAndKeepsCountWithoutNewViews() {
    Article article = saveArticle();
    redisRepository.recordViewIfUnique(
        article.getId(), "visitor-a", 0L, DEDUPLICATION_TTL, Instant.now(), Duration.ofHours(26));
    redisRepository.recordViewIfUnique(
        article.getId(), "visitor-b", 0L, DEDUPLICATION_TTL, Instant.now(), Duration.ofHours(26));

    flushService.flushPendingViews();

    assertThat(articleRepository.findById(article.getId()).orElseThrow().getViewCount())
        .isEqualTo(2L);
    assertThat(redisRepository.findViewCount(article.getId())).hasValue(2L);
    assertThat(redisRepository.findFlushingKeys()).isEmpty();
  }

  @Test
  void keepsCountWhenViewIsRecordedAfterBatchMove() {
    Article article = saveArticle();
    redisRepository.recordViewIfUnique(
        article.getId(), "visitor-a", 0L, DEDUPLICATION_TTL, Instant.now(), Duration.ofHours(26));
    redisRepository.moveActiveDeltaToFlushBatch();
    String flushingKey = redisRepository.findFlushingKeys().getFirst();
    ArticleViewRedisRepository.FlushBatch batch = redisRepository.getFlushBatch(flushingKey);

    redisRepository.recordViewIfUnique(
        article.getId(), "visitor-b", 0L, DEDUPLICATION_TTL, Instant.now(), Duration.ofHours(26));
    flushWriter.apply(batch);
    redisRepository.deleteFlushBatch(batch.key());

    assertThat(articleRepository.findById(article.getId()).orElseThrow().getViewCount())
        .isEqualTo(1L);
    assertThat(redisRepository.findViewCount(article.getId())).hasValue(2L);
    assertThat(redisRepository.findFlushingKeys()).isEmpty();
  }

  @Test
  void appliesSameFlushBatchOnlyOnce() {
    Article article = saveArticle();
    redisRepository.recordViewIfUnique(
        article.getId(), "visitor-a", 0L, DEDUPLICATION_TTL, Instant.now(), Duration.ofHours(26));
    redisRepository.moveActiveDeltaToFlushBatch();
    String flushingKey = redisRepository.findFlushingKeys().getFirst();
    ArticleViewRedisRepository.FlushBatch batch = redisRepository.getFlushBatch(flushingKey);

    ArticleViewFlushWriter.ApplyResult first = flushWriter.apply(batch);
    ArticleViewFlushWriter.ApplyResult second = flushWriter.apply(batch);

    assertThat(first).isEqualTo(ArticleViewFlushWriter.ApplyResult.APPLIED);
    assertThat(second).isEqualTo(ArticleViewFlushWriter.ApplyResult.ALREADY_APPLIED);
    assertThat(articleRepository.findById(article.getId()).orElseThrow().getViewCount())
        .isEqualTo(1L);
  }

  @Test
  void deletesOnlyExpiredFlushHistoryInConfiguredBatches() {
    Instant now = Instant.parse("2026-07-15T00:00:00Z");
    String prefix = "cleanup-" + UUID.randomUUID().toString().substring(0, 8);
    jdbcTemplate.update(
        "INSERT INTO article_view_flush_history (batch_id, created_at) VALUES (?, ?)",
        prefix + "-old-1",
        Timestamp.from(now.minus(Duration.ofDays(31))));
    jdbcTemplate.update(
        "INSERT INTO article_view_flush_history (batch_id, created_at) VALUES (?, ?)",
        prefix + "-old-2",
        Timestamp.from(now.minus(Duration.ofDays(31))));
    jdbcTemplate.update(
        "INSERT INTO article_view_flush_history (batch_id, created_at) VALUES (?, ?)",
        prefix + "-old-3",
        Timestamp.from(now.minus(Duration.ofDays(31))));
    jdbcTemplate.update(
        "INSERT INTO article_view_flush_history (batch_id, created_at) VALUES (?, ?)",
        prefix + "-old-protected",
        Timestamp.from(now.minus(Duration.ofDays(31))));
    jdbcTemplate.update(
        "INSERT INTO article_view_flush_history (batch_id, created_at) VALUES (?, ?)",
        prefix + "-recent",
        Timestamp.from(now.minus(Duration.ofDays(29))));
    redisTemplate.opsForHash().put("av:delta:flushing:" + prefix + "-old-protected", "1", "1");

    ArticleViewFlushHistoryCleanupService cleanupService =
        new ArticleViewFlushHistoryCleanupService(
            flushRepository,
            redisRepository,
            new ArticleViewFlushHistoryCleanupProperties(
                Duration.ofDays(30), Duration.ofDays(1), 2),
            Clock.fixed(now, ZoneOffset.UTC));

    cleanupService.deleteExpiredHistory();

    List<String> remainingBatchIds =
        jdbcTemplate.queryForList(
            "SELECT batch_id FROM article_view_flush_history WHERE batch_id LIKE ?",
            String.class,
            prefix + "%");
    assertThat(remainingBatchIds)
        .containsExactlyInAnyOrder(prefix + "-old-protected", prefix + "-recent");
  }

  private Article saveArticle() {
    String suffix = UUID.randomUUID().toString();
    String sourceName = "View Test " + suffix;
    sourceRepository.saveAndFlush(
        ArticleSource.create(
            sourceName,
            new SourcePolicy(
                new CrawlingPolicy(
                    List.of("https://example.com/feed-" + suffix),
                    List.of("example.com"),
                    List.of("article"),
                    List.of(),
                    List.of())),
            "en",
            SourceType.RSS,
            LicenseInfo.createCcBy("4.0")));
    return articleRepository.saveAndFlush(
        Article.create(
            "https://example.com/article-" + suffix,
            sourceName,
            null,
            "en",
            Instant.now(),
            LicenseInfo.createCcBy("4.0")));
  }
}

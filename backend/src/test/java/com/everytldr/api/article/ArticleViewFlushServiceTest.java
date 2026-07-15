package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
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
  void flushesDeltaToDatabaseAndKeepsCurrentRedisCount() {
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

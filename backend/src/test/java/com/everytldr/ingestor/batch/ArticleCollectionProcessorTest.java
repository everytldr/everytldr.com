package com.everytldr.ingestor.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.ingestion.IngestionExceptions;
import com.everytldr.ingestor.ingestion.IngestionMetrics;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.everytldr.ingestor.source.SourceClientRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleCollectionProcessorTest {

  private static final String FEED_URL = "https://feeds.example.com/rss.xml";
  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-08T08:25:43Z");

  @Test
  void recordsSuccessOutcome() throws Exception {
    SourceClientRegistry sourceClientRegistry = mock(SourceClientRegistry.class);
    SourceClient sourceClient = mock(SourceClient.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionProcessor processor =
        new ArticleCollectionProcessor(sourceClientRegistry, new IngestionMetrics(meterRegistry));
    ArticleCollectionTarget target = target();
    CollectedArticle collectedArticle = collectedArticle("https://news.example.com/success");
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(target)).thenReturn(List.of(collectedArticle));

    ArticleCollectionResult result = processor.process(target);

    assertThat(result).isEqualTo(new ArticleCollectionResult(target, List.of(collectedArticle)));
    assertThat(targetAttemptCount(meterRegistry, "success")).isEqualTo(1.0);
    assertThat(targetAttemptDurationCount(meterRegistry, "success")).isEqualTo(1);
  }

  @Test
  void recordsRetryableFailureOutcome() throws Exception {
    SourceClientRegistry sourceClientRegistry = mock(SourceClientRegistry.class);
    SourceClient sourceClient = mock(SourceClient.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionProcessor processor =
        new ArticleCollectionProcessor(sourceClientRegistry, new IngestionMetrics(meterRegistry));
    ArticleCollectionTarget target = target();
    IngestionExceptions.Retryable retryable =
        new IngestionExceptions.Retryable("temporary failure", new RuntimeException("boom"));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(target)).thenThrow(retryable);

    assertThatThrownBy(() -> processor.process(target)).isSameAs(retryable);

    assertThat(targetAttemptCount(meterRegistry, "retryable_failure")).isEqualTo(1.0);
    assertThat(targetAttemptDurationCount(meterRegistry, "retryable_failure")).isEqualTo(1);
  }

  @Test
  void recordsSkippableFailureOutcome() throws Exception {
    SourceClientRegistry sourceClientRegistry = mock(SourceClientRegistry.class);
    SourceClient sourceClient = mock(SourceClient.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionProcessor processor =
        new ArticleCollectionProcessor(sourceClientRegistry, new IngestionMetrics(meterRegistry));
    ArticleCollectionTarget target = target();
    IngestionExceptions.Skippable skippable =
        new IngestionExceptions.Skippable("invalid feed", new RuntimeException("boom"));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(target)).thenThrow(skippable);

    assertThatThrownBy(() -> processor.process(target)).isSameAs(skippable);

    assertThat(targetAttemptCount(meterRegistry, "skippable_failure")).isEqualTo(1.0);
    assertThat(targetAttemptDurationCount(meterRegistry, "skippable_failure")).isEqualTo(1);
  }

  @Test
  void recordsFatalFailureOutcome() {
    SourceClientRegistry sourceClientRegistry = mock(SourceClientRegistry.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionProcessor processor =
        new ArticleCollectionProcessor(sourceClientRegistry, new IngestionMetrics(meterRegistry));
    ArticleCollectionTarget target = target();
    when(sourceClientRegistry.getClient(SourceType.RSS))
        .thenThrow(new IllegalStateException("No SourceClient supports sourceType: RSS"));

    assertThatThrownBy(() -> processor.process(target))
        .isInstanceOf(IngestionExceptions.Fatal.class)
        .hasMessageContaining("Failed to collect articles");

    assertThat(targetAttemptCount(meterRegistry, "fatal_failure")).isEqualTo(1.0);
    assertThat(targetAttemptDurationCount(meterRegistry, "fatal_failure")).isEqualTo(1);
  }

  private double targetAttemptCount(SimpleMeterRegistry meterRegistry, String outcome) {
    return meterRegistry
        .get("everytldr.ingestor.article_collection.target.attempts")
        .tag("source_type", "rss")
        .tag("outcome", outcome)
        .counter()
        .count();
  }

  private long targetAttemptDurationCount(SimpleMeterRegistry meterRegistry, String outcome) {
    return meterRegistry
        .get("everytldr.ingestor.article_collection.target.attempt.duration")
        .tag("source_type", "rss")
        .tag("outcome", outcome)
        .timer()
        .count();
  }

  private ArticleCollectionTarget target() {
    ArticleSource source =
        ArticleSource.create(
            "Example News",
            new SourcePolicy(
                new CrawlingPolicy(
                    List.of(FEED_URL),
                    List.of("news.example.com"),
                    List.of("article"),
                    List.of(),
                    List.of())),
            "en",
            SourceType.RSS,
            LicenseInfo.createCcBy("4.0"));
    return new ArticleCollectionTarget(source, FEED_URL);
  }

  private CollectedArticle collectedArticle(String contentUrl) {
    return new CollectedArticle(
        contentUrl, "Example News", null, "en", PUBLISHED_AT, LicenseInfo.createCcBy("4.0"));
  }
}

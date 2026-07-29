package com.everytldr.ingestor.ingestion;

import static com.everytldr.common.domain.source.SourceType.RSS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class IngestionMetricsTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final IngestionMetrics metrics = new IngestionMetrics(meterRegistry);

  @Test
  void recordsArticleCollectionMetrics() {
    metrics.recordArticles(1, 1, 1, 1, 1, 1, 1);
    metrics.recordArticleCollectionJobStart("started");
    metrics.recordArticleCollectionStepCompletion("COMPLETED", "COMPLETED");
    metrics.recordArticleCollectionTargetAttempt(RSS, "success");
    metrics.recordArticleCollectionTargetAttemptDuration(RSS, "success", Duration.ofMillis(10));

    assertCounterCount("everytldr.ingestor.articles", "result", "received");
    assertCounterCount("everytldr.ingestor.article_collection.job.starts", "outcome", "started");
    assertCounterCount(
        "everytldr.ingestor.article_collection.step.completions", "status", "completed");
    assertCounterCount(
        "everytldr.ingestor.article_collection.target.attempts", "source_type", "rss");
    assertThat(
            meterRegistry
                .get("everytldr.ingestor.article_collection.target.attempt.duration")
                .tag("source_type", "rss")
                .tag("outcome", "success")
                .timer()
                .count())
        .isEqualTo(1);
  }

  @Test
  void ignoresMeterRegistryFailures() {
    ThrowingMeterRegistry throwingMeterRegistry = new ThrowingMeterRegistry();
    IngestionMetrics throwingMetrics = new IngestionMetrics(throwingMeterRegistry);
    throwingMeterRegistry.failOnRecord();

    assertThatCode(() -> throwingMetrics.recordArticles(1, 1, 1, 1, 1, 1, 1))
        .doesNotThrowAnyException();
    assertThatCode(() -> throwingMetrics.recordArticleCollectionJobStart("started"))
        .doesNotThrowAnyException();
    assertThatCode(
            () -> throwingMetrics.recordArticleCollectionStepCompletion("COMPLETED", "COMPLETED"))
        .doesNotThrowAnyException();
    assertThatCode(() -> throwingMetrics.recordArticleCollectionTargetAttempt(RSS, "success"))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                throwingMetrics.recordArticleCollectionTargetAttemptDuration(
                    RSS, "success", Duration.ofMillis(10)))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsInvalidMetricArguments() {
    assertThatThrownBy(() -> metrics.recordArticleCollectionJobStart(" "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> metrics.recordArticleCollectionStepCompletion(null, "COMPLETED"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> metrics.recordArticleCollectionTargetAttempt(null, "success"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                metrics.recordArticleCollectionTargetAttemptDuration(
                    RSS, "success", Duration.ofMillis(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void assertCounterCount(String metric, String tag, String value) {
    assertThat(meterRegistry.get(metric).tag(tag, value).counter().count()).isEqualTo(1);
  }

  private static class ThrowingMeterRegistry extends SimpleMeterRegistry {

    private boolean failsOnRecord;

    void failOnRecord() {
      failsOnRecord = true;
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
      if (failsOnRecord) {
        throw new IllegalStateException("meter registry failure");
      }
      return super.newCounter(id);
    }

    @Override
    protected Timer newTimer(
        Meter.Id id,
        DistributionStatisticConfig distributionStatisticConfig,
        PauseDetector pauseDetector) {
      if (failsOnRecord) {
        throw new IllegalStateException("meter registry failure");
      }
      return super.newTimer(id, distributionStatisticConfig, pauseDetector);
    }
  }
}

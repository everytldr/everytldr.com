package com.everytldr.enricher.processor;

import static com.everytldr.enricher.processor.EnricherMetrics.ExternalStage.CONTENT_RESOLUTION;
import static com.everytldr.enricher.processor.EnricherMetrics.ExternalStageOutcome.RETRYABLE_FAILURE;
import static com.everytldr.enricher.processor.ProcessingResult.Status.SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EnricherMetricsTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final EnricherMetrics metrics =
      new EnricherMetrics(meterRegistry, Mockito.mock(ArticleIngestionJobRepository.class));

  @Test
  void recordsPollingJobAndExternalStageMetrics() {
    metrics.recordPolling(Duration.ofMillis(10), true);
    metrics.recordJob(SUCCEEDED, Duration.ofMillis(20));
    metrics.recordExternalStage(CONTENT_RESOLUTION, RETRYABLE_FAILURE, Duration.ofMillis(30));

    assertCounterCount("everytldr.enricher.polling.runs", "outcome", "success");
    assertTimerCount("everytldr.enricher.polling.duration", "outcome", "success");
    assertCounterCount("everytldr.enricher.jobs", "status", "succeeded");
    assertTimerCount("everytldr.enricher.job.attempt.duration", "status", "succeeded");
    assertThat(
            meterRegistry
                .get("everytldr.enricher.external.stage.duration")
                .tag("stage", "content_resolution")
                .tag("outcome", "retryable_failure")
                .timer()
                .count())
        .isEqualTo(1);
  }

  @Test
  void ignoresMeterRegistryFailures() {
    ThrowingMeterRegistry throwingMeterRegistry = new ThrowingMeterRegistry();
    EnricherMetrics throwingMetrics =
        new EnricherMetrics(
            throwingMeterRegistry, Mockito.mock(ArticleIngestionJobRepository.class));
    throwingMeterRegistry.failOnRecord();

    assertThatCode(() -> throwingMetrics.recordPolling(Duration.ofMillis(10), true))
        .doesNotThrowAnyException();
    assertThatCode(() -> throwingMetrics.recordJob(SUCCEEDED, Duration.ofMillis(20)))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                throwingMetrics.recordExternalStage(
                    CONTENT_RESOLUTION, RETRYABLE_FAILURE, Duration.ofMillis(30)))
        .doesNotThrowAnyException();
  }

  private void assertCounterCount(String metric, String tag, String value) {
    assertThat(meterRegistry.get(metric).tag(tag, value).counter().count()).isEqualTo(1);
  }

  private void assertTimerCount(String metric, String tag, String value) {
    assertThat(meterRegistry.get(metric).tag(tag, value).timer().count()).isEqualTo(1);
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

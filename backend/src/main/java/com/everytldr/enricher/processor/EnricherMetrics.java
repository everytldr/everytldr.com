package com.everytldr.enricher.processor;

import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("enricher")
@Slf4j
public class EnricherMetrics {

  private static final String JOBS_METRIC = "everytldr.enricher.jobs";
  private static final String BACKLOG_METRIC = "everytldr.enricher.jobs.backlog";
  private static final String POLLING_RUNS_METRIC = "everytldr.enricher.polling.runs";
  private static final String POLLING_DURATION_METRIC = "everytldr.enricher.polling.duration";
  private static final String JOB_ATTEMPT_DURATION_METRIC =
      "everytldr.enricher.job.attempt.duration";
  private static final String EXTERNAL_STAGE_DURATION_METRIC =
      "everytldr.enricher.external.stage.duration";

  private final MeterRegistry meterRegistry;

  public EnricherMetrics(
      MeterRegistry meterRegistry, ArticleIngestionJobRepository articleIngestionJobRepository) {
    this.meterRegistry = meterRegistry;
    registerBacklogGauge(articleIngestionJobRepository, IngestionState.PENDING);
    registerBacklogGauge(articleIngestionJobRepository, IngestionState.RETRY_SCHEDULED);
    registerBacklogGauge(articleIngestionJobRepository, IngestionState.PROCESSING);
  }

  public void recordPolling(Duration duration, boolean succeeded) {
    assertValidDuration(duration);
    String outcome = succeeded ? "success" : "failure";
    recordSafely(
        POLLING_RUNS_METRIC,
        () -> meterRegistry.counter(POLLING_RUNS_METRIC, "outcome", outcome).increment());
    recordDuration(POLLING_DURATION_METRIC, duration, "outcome", outcome);
  }

  public void recordJob(ProcessingResult.Status status, Duration duration) {
    Objects.requireNonNull(status, "status must not be null");
    assertValidDuration(duration);
    String statusTag = toTagValue(status.name());
    recordSafely(
        JOBS_METRIC, () -> meterRegistry.counter(JOBS_METRIC, "status", statusTag).increment());
    recordDuration(JOB_ATTEMPT_DURATION_METRIC, duration, "status", statusTag);
  }

  public void recordExternalStage(
      ExternalStage stage, ExternalStageOutcome outcome, Duration duration) {
    Objects.requireNonNull(stage, "stage must not be null");
    Objects.requireNonNull(outcome, "outcome must not be null");
    assertValidDuration(duration);
    recordDuration(
        EXTERNAL_STAGE_DURATION_METRIC,
        duration,
        "stage",
        toTagValue(stage.name()),
        "outcome",
        toTagValue(outcome.name()));
  }

  private void registerBacklogGauge(
      ArticleIngestionJobRepository articleIngestionJobRepository, IngestionState state) {
    Gauge.builder(
            BACKLOG_METRIC,
            articleIngestionJobRepository,
            repository -> repository.countByState(state))
        .tag("state", toTagValue(state.name()))
        .register(meterRegistry);
  }

  private String toTagValue(String value) {
    return value.toLowerCase(Locale.ROOT);
  }

  private void recordDuration(String metric, Duration duration, String... tags) {
    recordSafely(
        metric, () -> Timer.builder(metric).tags(tags).register(meterRegistry).record(duration));
  }

  private void assertValidDuration(Duration duration) {
    Objects.requireNonNull(duration, "duration must not be null");
    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration must not be negative");
    }
  }

  private void recordSafely(String metric, Runnable recorder) {
    try {
      recorder.run();
    } catch (RuntimeException e) {
      log.warn("Failed to record enricher metric. metric={}", metric, e);
    }
  }

  public enum ExternalStage {
    CONTENT_RESOLUTION,
    ENRICHMENT
  }

  public enum ExternalStageOutcome {
    SUCCESS,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
  }
}

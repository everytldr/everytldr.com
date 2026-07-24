package com.everytldr.ingestor.ingestion;

import com.everytldr.common.domain.source.SourceType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionMetrics {

  private static final String ARTICLES_METRIC = "everytldr.ingestor.articles";
  private static final String ARTICLE_COLLECTION_JOB_STARTS_METRIC =
      "everytldr.ingestor.article_collection.job.starts";
  private static final String ARTICLE_COLLECTION_STEP_COMPLETIONS_METRIC =
      "everytldr.ingestor.article_collection.step.completions";
  private static final String ARTICLE_COLLECTION_TARGET_ATTEMPTS_METRIC =
      "everytldr.ingestor.article_collection.target.attempts";
  private static final String ARTICLE_COLLECTION_TARGET_ATTEMPT_DURATION_METRIC =
      "everytldr.ingestor.article_collection.target.attempt.duration";

  private final MeterRegistry meterRegistry;

  public void recordArticles(
      int received,
      int valid,
      int invalidSkipped,
      int duplicateInBatchSkipped,
      int existingDuplicateSkipped,
      int concurrencyDuplicateSkipped,
      int saved) {
    recordArticleCount("received", received);
    recordArticleCount("valid", valid);
    recordArticleCount("invalid_skipped", invalidSkipped);
    recordArticleCount("duplicate_in_batch_skipped", duplicateInBatchSkipped);
    recordArticleCount("existing_duplicate_skipped", existingDuplicateSkipped);
    recordArticleCount("concurrency_duplicate_skipped", concurrencyDuplicateSkipped);
    recordArticleCount("saved", saved);
  }

  public void recordArticleCollectionJobStart(String outcome) {
    assertOutcome(outcome);
    recordSafely(
        ARTICLE_COLLECTION_JOB_STARTS_METRIC,
        () ->
            meterRegistry
                .counter(ARTICLE_COLLECTION_JOB_STARTS_METRIC, "outcome", outcome)
                .increment());
  }

  public void recordArticleCollectionStepCompletion(String status, String exitCode) {
    String statusTag = normalizeTagValue(status, "status");
    String exitCodeTag = normalizeTagValue(exitCode, "exitCode");
    recordSafely(
        ARTICLE_COLLECTION_STEP_COMPLETIONS_METRIC,
        () ->
            meterRegistry
                .counter(
                    ARTICLE_COLLECTION_STEP_COMPLETIONS_METRIC,
                    "status",
                    statusTag,
                    "exit_code",
                    exitCodeTag)
                .increment());
  }

  public void recordArticleCollectionTargetAttempt(SourceType sourceType, String outcome) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    assertOutcome(outcome);

    String sourceTypeTag = sourceType.name().toLowerCase(Locale.ROOT);
    recordSafely(
        ARTICLE_COLLECTION_TARGET_ATTEMPTS_METRIC,
        () ->
            meterRegistry
                .counter(
                    ARTICLE_COLLECTION_TARGET_ATTEMPTS_METRIC,
                    "source_type",
                    sourceTypeTag,
                    "outcome",
                    outcome)
                .increment());
  }

  public void recordArticleCollectionTargetAttemptDuration(
      SourceType sourceType, String outcome, Duration duration) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    assertOutcome(outcome);
    Objects.requireNonNull(duration, "duration must not be null");
    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration must not be negative");
    }

    String sourceTypeTag = sourceType.name().toLowerCase(Locale.ROOT);
    recordSafely(
        ARTICLE_COLLECTION_TARGET_ATTEMPT_DURATION_METRIC,
        () ->
            Timer.builder(ARTICLE_COLLECTION_TARGET_ATTEMPT_DURATION_METRIC)
                .tag("source_type", sourceTypeTag)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(duration));
  }

  private void recordArticleCount(String result, int count) {
    if (count > 0) {
      recordSafely(
          ARTICLES_METRIC,
          () -> meterRegistry.counter(ARTICLES_METRIC, "result", result).increment(count));
    }
  }

  private void recordSafely(String metric, Runnable recorder) {
    try {
      recorder.run();
    } catch (RuntimeException e) {
      log.warn("Failed to record ingestor metric. metric={}", metric, e);
    }
  }

  private void assertOutcome(String outcome) {
    Objects.requireNonNull(outcome, "outcome must not be null");
    if (outcome.isBlank()) {
      throw new IllegalArgumentException("outcome must not be blank");
    }
  }

  private String normalizeTagValue(String value, String name) {
    Objects.requireNonNull(value, "%s must not be null".formatted(name));
    if (value.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(name));
    }
    return value.toLowerCase(Locale.ROOT);
  }
}

package com.everytldr.ingestor.ingestion;

import com.everytldr.common.domain.source.SourceType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IngestionMetrics {

  private static final String ARTICLES_METRIC = "everytldr.ingestor.articles";
  private static final String SOURCES_METRIC = "everytldr.ingestor.sources";
  private static final String SOURCE_DURATION_METRIC = "everytldr.ingestor.source.duration";

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

  public void recordSource(SourceType sourceType, boolean successful, Duration duration) {
    String sourceTypeTag = sourceType.name().toLowerCase(Locale.ROOT);
    String outcome = successful ? "success" : "failure";
    meterRegistry
        .counter(SOURCES_METRIC, "source_type", sourceTypeTag, "outcome", outcome)
        .increment();
    Timer.builder(SOURCE_DURATION_METRIC)
        .tag("source_type", sourceTypeTag)
        .tag("outcome", outcome)
        .register(meterRegistry)
        .record(duration);
  }

  private void recordArticleCount(String result, int count) {
    if (count > 0) {
      meterRegistry.counter(ARTICLES_METRIC, "result", result).increment(count);
    }
  }
}

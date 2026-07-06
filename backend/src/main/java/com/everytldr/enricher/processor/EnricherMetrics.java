package com.everytldr.enricher.processor;

import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("enricher")
public class EnricherMetrics {

  private static final String JOBS_METRIC = "everytldr.enricher.jobs";
  private static final String BACKLOG_METRIC = "everytldr.enricher.jobs.backlog";

  private final MeterRegistry meterRegistry;

  public EnricherMetrics(
      MeterRegistry meterRegistry, ArticleIngestionJobRepository articleIngestionJobRepository) {
    this.meterRegistry = meterRegistry;
    registerBacklogGauge(articleIngestionJobRepository, IngestionState.PENDING);
    registerBacklogGauge(articleIngestionJobRepository, IngestionState.RETRY_SCHEDULED);
    registerBacklogGauge(articleIngestionJobRepository, IngestionState.PROCESSING);
  }

  public void recordJob(ProcessingResult.Status status) {
    meterRegistry.counter(JOBS_METRIC, "status", toTagValue(status.name())).increment();
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
}

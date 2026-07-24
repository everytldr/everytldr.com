package com.everytldr.enricher.scheduler;

import static com.everytldr.enricher.processor.ProcessingResult.Status.RETRY_SCHEDULED;
import static com.everytldr.enricher.processor.ProcessingResult.Status.SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.enricher.processor.EnricherMetrics;
import com.everytldr.enricher.processor.JobProcessor;
import com.everytldr.enricher.processor.ProcessingProperties;
import com.everytldr.enricher.processor.ProcessingResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PollingSchedulerTest {

  private final JobProcessor jobProcessor = org.mockito.Mockito.mock(JobProcessor.class);
  private final ArticleIngestionJobRepository jobRepository =
      org.mockito.Mockito.mock(ArticleIngestionJobRepository.class);
  private SimpleMeterRegistry meterRegistry;
  private EnricherMetrics enricherMetrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    enricherMetrics = new EnricherMetrics(meterRegistry, jobRepository);
  }

  @Test
  void runsProcessorWithConfiguredBatchSize() {
    PollingScheduler scheduler = scheduler(3);
    when(jobProcessor.processNextBatch(3))
        .thenReturn(
            List.of(
                new ProcessingResult(100L, SUCCEEDED),
                new ProcessingResult(101L, RETRY_SCHEDULED)));

    scheduler.runPolling();

    verify(jobProcessor).processNextBatch(3);
    assertPollingMetric("success");
  }

  @Test
  void doesNotPropagateProcessorFailure() {
    PollingScheduler scheduler = scheduler(5);
    when(jobProcessor.processNextBatch(5))
        .thenThrow(new IllegalStateException("processor failure"));

    assertThatCode(scheduler::runPolling).doesNotThrowAnyException();
    assertPollingMetric("failure");
  }

  private PollingScheduler scheduler(int batchSize) {
    return new PollingScheduler(
        jobProcessor,
        new ProcessingProperties(
            true,
            batchSize,
            Duration.ofSeconds(30),
            3,
            new ProcessingProperties.RetryProperties(
                Duration.ofMinutes(1), 2.0, Duration.ofMinutes(10)),
            Duration.ofMinutes(15)),
        enricherMetrics);
  }

  private void assertPollingMetric(String outcome) {
    assertThat(
            meterRegistry
                .get("everytldr.enricher.polling.runs")
                .tag("outcome", outcome)
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(
            meterRegistry
                .get("everytldr.enricher.polling.duration")
                .tag("outcome", outcome)
                .timer()
                .count())
        .isEqualTo(1);
  }
}

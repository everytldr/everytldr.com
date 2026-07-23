package com.everytldr.enricher.scheduler;

import static com.everytldr.enricher.processor.ProcessingResult.Status.RETRY_SCHEDULED;
import static com.everytldr.enricher.processor.ProcessingResult.Status.SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.enricher.processor.JobProcessor;
import com.everytldr.enricher.processor.ProcessingProperties;
import com.everytldr.enricher.processor.ProcessingResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PollingSchedulerTest {

  private final JobProcessor jobProcessor = org.mockito.Mockito.mock(JobProcessor.class);

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
  }

  @Test
  void doesNotPropagateProcessorFailure() {
    PollingScheduler scheduler = scheduler(5);
    when(jobProcessor.processNextBatch(5))
        .thenThrow(new IllegalStateException("processor failure"));

    assertThatCode(scheduler::runPolling).doesNotThrowAnyException();
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
            Duration.ofMinutes(15)));
  }
}

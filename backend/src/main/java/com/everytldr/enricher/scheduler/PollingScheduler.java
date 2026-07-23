package com.everytldr.enricher.scheduler;

import com.everytldr.enricher.processor.EnricherMetrics;
import com.everytldr.enricher.processor.JobProcessor;
import com.everytldr.enricher.processor.ProcessingProperties;
import com.everytldr.enricher.processor.ProcessingResult;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("enricherPollingScheduler")
@RequiredArgsConstructor
@Profile("enricher")
@ConditionalOnProperty(name = "everytldr.enricher.processing.enabled", havingValue = "true")
@Slf4j
public class PollingScheduler {

  private final JobProcessor articleJobProcessor;
  private final ProcessingProperties properties;
  private final EnricherMetrics enricherMetrics;

  @Scheduled(fixedDelayString = "${everytldr.enricher.processing.fixed-delay}")
  void runPolling() {
    long startedAtNanos = System.nanoTime();
    try {
      List<ProcessingResult> results = articleJobProcessor.processNextBatch(properties.batchSize());
      enricherMetrics.recordPolling(elapsedSince(startedAtNanos), true);
      log.info(
          "Finished article enrichment polling. requested={}, processed={}",
          properties.batchSize(),
          results.size());
    } catch (RuntimeException e) {
      enricherMetrics.recordPolling(elapsedSince(startedAtNanos), false);
      log.warn("Failed to run article enrichment polling. batchSize={}", properties.batchSize(), e);
    }
  }

  private Duration elapsedSince(long startedAtNanos) {
    return Duration.ofNanos(System.nanoTime() - startedAtNanos);
  }
}

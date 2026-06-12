package com.everytldr.enricher.scheduler;

import com.everytldr.enricher.processor.JobProcessor;
import com.everytldr.enricher.processor.ProcessingProperties;
import com.everytldr.enricher.processor.ProcessingResult;
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

  @Scheduled(fixedDelayString = "${everytldr.enricher.processing.fixed-delay}")
  void runPolling() {
    try {
      List<ProcessingResult> results = articleJobProcessor.processNextBatch(properties.batchSize());
      log.info(
          "Finished article enrichment polling. requested={}, processed={}",
          properties.batchSize(),
          results.size());
    } catch (RuntimeException e) {
      log.warn("Failed to run article enrichment polling. batchSize={}", properties.batchSize(), e);
    }
  }
}

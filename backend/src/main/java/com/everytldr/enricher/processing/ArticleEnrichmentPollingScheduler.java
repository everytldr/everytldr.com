package com.everytldr.enricher.processing;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("enricher")
@ConditionalOnProperty(name = "everytldr.enricher.processing.enabled", havingValue = "true")
@Slf4j
public class ArticleEnrichmentPollingScheduler {

  private final ArticleEnrichmentJobProcessor articleEnrichmentJobProcessor;
  private final EnricherProcessingProperties properties;

  @Scheduled(fixedDelayString = "${everytldr.enricher.processing.fixed-delay}")
  void runArticleEnrichmentPolling() {
    try {
      List<ArticleEnrichmentProcessingResult> results =
          articleEnrichmentJobProcessor.processNextJobs(properties.batchSize());
      log.info(
          "Finished article enrichment polling. requested={}, processed={}, statusCounts={}",
          properties.batchSize(),
          results.size(),
          countByStatus(results));
    } catch (RuntimeException e) {
      log.warn("Failed to run article enrichment polling. batchSize={}", properties.batchSize(), e);
    }
  }

  private Map<ArticleEnrichmentProcessingStatus, Long> countByStatus(
      List<ArticleEnrichmentProcessingResult> results) {
    return results.stream()
        .collect(
            Collectors.groupingBy(
                ArticleEnrichmentProcessingResult::status,
                () -> new EnumMap<>(ArticleEnrichmentProcessingStatus.class),
                Collectors.counting()));
  }
}

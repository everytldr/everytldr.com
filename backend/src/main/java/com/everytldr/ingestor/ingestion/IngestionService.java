package com.everytldr.ingestor.ingestion;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.everytldr.ingestor.source.SourceClientRegistry;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

  private final ArticleSourceRepository articleSourceRepository;

  private final SourceClientRegistry sourceClientRegistry;

  private final CollectedArticleSaveService collectedArticleSaveService;

  private final IngestionMetrics ingestionMetrics;

  public IngestionSummary ingestActiveSources() {
    List<ArticleSource> sources = articleSourceRepository.findAllByIsActiveTrue();
    int failedSources = 0;

    for (ArticleSource source : sources) {
      long startedAt = System.nanoTime();
      try {
        SourceClient client = sourceClientRegistry.getClient(source.getSourceType());
        List<CollectedArticle> collectedArticles = client.collect(source);
        log.info(
            "Collected articles from source. sourceId={}, sourceName={}, sourceType={}, collected={}",
            source.getId(),
            source.getName(),
            source.getSourceType(),
            collectedArticles == null ? 0 : collectedArticles.size());
        collectedArticleSaveService.saveNewArticles(collectedArticles);
        ingestionMetrics.recordSource(
            source.getSourceType(), true, Duration.ofNanos(System.nanoTime() - startedAt));
      } catch (RuntimeException e) {
        failedSources++;
        ingestionMetrics.recordSource(
            source.getSourceType(), false, Duration.ofNanos(System.nanoTime() - startedAt));
        log.warn(
            "Failed to ingest article source. sourceId={}, sourceName={}, sourceType={}",
            source.getId(),
            source.getName(),
            source.getSourceType(),
            e);
      }
    }

    return new IngestionSummary(sources.size(), failedSources);
  }

  public record IngestionSummary(int sourcesProcessed, int sourcesFailed) {
    public boolean isCompleteFailure() {
      return sourcesProcessed > 0 && sourcesFailed == sourcesProcessed;
    }
  }
}

package com.everytldr.ingestor.ingestion;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.everytldr.ingestor.source.SourceClientRegistry;
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

  public void ingestActiveSources() {
    List<ArticleSource> sources = articleSourceRepository.findAllByIsActiveTrue();

    for (ArticleSource source : sources) {
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
      } catch (RuntimeException e) {
        log.warn(
            "Failed to ingest article source. sourceId={}, sourceName={}, sourceType={}",
            source.getId(),
            source.getName(),
            source.getSourceType(),
            e);
      }
    }
  }
}

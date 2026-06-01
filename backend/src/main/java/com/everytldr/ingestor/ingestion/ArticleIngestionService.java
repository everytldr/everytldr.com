package com.everytldr.ingestor.ingestion;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.ingestor.provider.ArticleSourceClient;
import com.everytldr.ingestor.provider.ArticleSourceClientRegistry;
import com.everytldr.ingestor.provider.CollectedArticle;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleIngestionService {

  private final ArticleSourceRepository articleSourceRepository;

  private final ArticleSourceClientRegistry articleSourceClientRegistry;

  private final CollectedArticleSaveService collectedArticleSaveService;

  public void ingestActiveSources() {
    List<ArticleSource> sources = articleSourceRepository.findAllByIsActiveTrue();

    for (ArticleSource source : sources) {
      try {
        ArticleSourceClient client = articleSourceClientRegistry.getClient(source.getSourceType());
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

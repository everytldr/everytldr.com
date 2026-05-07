package org.tldrtimes.ingestor.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.common.domain.source.ArticleSourceRepository;
import org.tldrtimes.ingestor.provider.ArticleSourceClient;
import org.tldrtimes.ingestor.provider.ArticleSourceClientRegistry;
import org.tldrtimes.ingestor.provider.CollectedArticle;

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

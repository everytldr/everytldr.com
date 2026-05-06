package org.tldrtimes.ingestor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.common.domain.source.ArticleSourceRepository;
import org.tldrtimes.ingestor.provider.ArticleSourceClient;
import org.tldrtimes.ingestor.provider.ArticleSourceClientRegistry;
import org.tldrtimes.ingestor.provider.CollectedArticle;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleIngestionService {

  private final ArticleSourceRepository articleSourceRepository;

  private final ArticleSourceClientRegistry articleSourceClientRegistry;

  private final CollectedArticleSaveService collectedArticleSaveService;

  public void ingestActiveSources() {
    List<ArticleSource> sources = articleSourceRepository.findAllByIsActiveTrue();

    for (ArticleSource source : sources) {
      ArticleSourceClient client = articleSourceClientRegistry.getClient(source.getSourceType());
      List<CollectedArticle> collectedArticles = client.collect(source);
      collectedArticleSaveService.saveNewArticles(collectedArticles);
    }
  }
}

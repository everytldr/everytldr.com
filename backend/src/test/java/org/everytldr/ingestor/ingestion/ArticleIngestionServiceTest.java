package org.everytldr.ingestor.ingestion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.everytldr.common.domain.source.ArticleSource;
import org.everytldr.common.domain.source.ArticleSourceRepository;
import org.everytldr.common.domain.source.SourceType;
import org.everytldr.ingestor.provider.ArticleSourceClient;
import org.everytldr.ingestor.provider.ArticleSourceClientRegistry;
import org.everytldr.ingestor.provider.CollectedArticle;

class ArticleIngestionServiceTest {

  @Test
  void continuesWithNextSourceWhenOneSourceFails() {
    ArticleSourceRepository articleSourceRepository = mock(ArticleSourceRepository.class);
    ArticleSourceClientRegistry articleSourceClientRegistry =
        mock(ArticleSourceClientRegistry.class);
    CollectedArticleSaveService collectedArticleSaveService =
        mock(CollectedArticleSaveService.class);
    ArticleIngestionService articleIngestionService =
        new ArticleIngestionService(
            articleSourceRepository, articleSourceClientRegistry, collectedArticleSaveService);
    ArticleSource firstSource =
        ArticleSource.create(
            "Broken Guardian",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);
    ArticleSource secondSource =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);
    ArticleSourceClient failingClient = mock(ArticleSourceClient.class);
    ArticleSourceClient succeedingClient = mock(ArticleSourceClient.class);
    CollectedArticle collectedArticle =
        new CollectedArticle(
            "https://www.theguardian.com/football/example",
            "The Guardian Football",
            null,
            "en",
            Instant.parse("2026-05-07T10:15:30Z"));

    when(articleSourceRepository.findAllByIsActiveTrue())
        .thenReturn(List.of(firstSource, secondSource));
    when(articleSourceClientRegistry.getClient(SourceType.GUARDIAN_API))
        .thenReturn(failingClient, succeedingClient);
    when(failingClient.collect(firstSource)).thenThrow(new IllegalStateException("boom"));
    when(succeedingClient.collect(secondSource)).thenReturn(List.of(collectedArticle));

    articleIngestionService.ingestActiveSources();

    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
  }
}

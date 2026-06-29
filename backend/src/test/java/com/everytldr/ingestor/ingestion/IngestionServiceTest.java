package com.everytldr.ingestor.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.everytldr.ingestor.source.SourceClientRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IngestionServiceTest {

  @Test
  void continuesWithNextSourceWhenOneSourceFails() {
    ArticleSourceRepository articleSourceRepository = mock(ArticleSourceRepository.class);
    SourceClientRegistry sourceClientRegistry = mock(SourceClientRegistry.class);
    CollectedArticleSaveService collectedArticleSaveService =
        mock(CollectedArticleSaveService.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    IngestionService ingestionService =
        new IngestionService(
            articleSourceRepository,
            sourceClientRegistry,
            collectedArticleSaveService,
            new IngestionMetrics(meterRegistry));
    ArticleSource firstSource =
        ArticleSource.create(
            "Broken Guardian",
            new SourcePolicy(
                new CrawlingPolicy(
                    List.of("https://content.guardianapis.com/search?section=football"),
                    List.of("theguardian.com", "www.theguardian.com"),
                    List.of("article"),
                    List.of())),
            "en",
            SourceType.RSS);
    ArticleSource secondSource =
        ArticleSource.create(
            "The Guardian Football",
            new SourcePolicy(
                new CrawlingPolicy(
                    List.of("https://content.guardianapis.com/search?section=football"),
                    List.of("theguardian.com", "www.theguardian.com"),
                    List.of("article"),
                    List.of())),
            "en",
            SourceType.RSS);
    SourceClient failingClient = mock(SourceClient.class);
    SourceClient succeedingClient = mock(SourceClient.class);
    CollectedArticle collectedArticle =
        new CollectedArticle(
            "https://www.theguardian.com/football/example",
            "The Guardian Football",
            null,
            "en",
            Instant.parse("2026-05-07T10:15:30Z"));

    when(articleSourceRepository.findAllByIsActiveTrue())
        .thenReturn(List.of(firstSource, secondSource));
    when(sourceClientRegistry.getClient(SourceType.RSS))
        .thenReturn(failingClient, succeedingClient);
    when(failingClient.collect(firstSource)).thenThrow(new IllegalStateException("boom"));
    when(succeedingClient.collect(secondSource)).thenReturn(List.of(collectedArticle));

    ingestionService.ingestActiveSources();

    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
    assertThat(
            meterRegistry
                .get("everytldr.ingestor.sources")
                .tag("source_type", "rss")
                .tag("outcome", "failure")
                .counter()
                .count())
        .isEqualTo(1.0);
    assertThat(
            meterRegistry
                .get("everytldr.ingestor.sources")
                .tag("source_type", "rss")
                .tag("outcome", "success")
                .counter()
                .count())
        .isEqualTo(1.0);
  }
}

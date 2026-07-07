package com.everytldr.ingestor.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.ingestion.CollectedArticleSaveService;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import com.everytldr.ingestor.source.CollectedArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class CollectedArticleWriterTest {

  private static final String FEED_URL = "https://feeds.example.com/rss.xml";
  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-08T08:25:43Z");

  @Test
  void savesCollectedArticles() throws Exception {
    CollectedArticleSaveService collectedArticleSaveService =
        mock(CollectedArticleSaveService.class);
    CollectedArticleWriter writer = new CollectedArticleWriter(collectedArticleSaveService);
    CollectedArticle firstArticle = collectedArticle("https://news.example.com/first");
    CollectedArticle secondArticle = collectedArticle("https://news.example.com/second");

    writer.write(new Chunk<>(List.of(result(List.of(firstArticle, secondArticle)))));

    verify(collectedArticleSaveService).saveNewArticles(List.of(firstArticle, secondArticle));
  }

  @Test
  void skipsSaveWhenChunkHasNoCollectedArticles() throws Exception {
    CollectedArticleSaveService collectedArticleSaveService =
        mock(CollectedArticleSaveService.class);
    CollectedArticleWriter writer = new CollectedArticleWriter(collectedArticleSaveService);

    writer.write(new Chunk<>(List.of(result(List.of()))));

    verify(collectedArticleSaveService, never()).saveNewArticles(any());
  }

  private ArticleCollectionResult result(List<CollectedArticle> collectedArticles) {
    ArticleSource source =
        ArticleSource.create(
            "Example News",
            new SourcePolicy(
                new CrawlingPolicy(
                    List.of(FEED_URL),
                    List.of("news.example.com"),
                    List.of("article"),
                    List.of(),
                    List.of())),
            "en",
            SourceType.RSS,
            LicenseInfo.createCcBy("4.0"));
    return new ArticleCollectionResult(
        new ArticleCollectionTarget(source, FEED_URL), collectedArticles);
  }

  private CollectedArticle collectedArticle(String contentUrl) {
    return new CollectedArticle(
        contentUrl, "Example News", null, "en", PUBLISHED_AT, LicenseInfo.createCcBy("4.0"));
  }
}

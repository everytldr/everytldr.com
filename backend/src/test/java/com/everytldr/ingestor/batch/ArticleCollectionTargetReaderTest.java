package com.everytldr.ingestor.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;

class ArticleCollectionTargetReaderTest {

  @Test
  void readsActiveSourceFeedUrlsInRepositoryOrder() {
    ArticleSourceRepository articleSourceRepository = mock(ArticleSourceRepository.class);
    ArticleSource firstSource = source("First News", "https://feeds.example.com/first.xml");
    ArticleSource secondSource =
        source(
            "Second News",
            "https://feeds.example.com/second-a.xml",
            "https://feeds.example.com/second-b.xml");
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc())
        .thenReturn(List.of(firstSource, secondSource));
    ArticleCollectionTargetReader reader =
        new ArticleCollectionTargetReader(articleSourceRepository);

    reader.open(new ExecutionContext());

    assertThat(reader.read())
        .isEqualTo(new ArticleCollectionTarget(firstSource, "https://feeds.example.com/first.xml"));
    assertThat(reader.read())
        .isEqualTo(
            new ArticleCollectionTarget(secondSource, "https://feeds.example.com/second-a.xml"));
    assertThat(reader.read())
        .isEqualTo(
            new ArticleCollectionTarget(secondSource, "https://feeds.example.com/second-b.xml"));
    assertThat(reader.read()).isNull();
    verify(articleSourceRepository, times(1)).findAllByIsActiveTrueOrderByIdAsc();
  }

  @Test
  void resumesFromSavedNextIndex() {
    ArticleSourceRepository articleSourceRepository = mock(ArticleSourceRepository.class);
    ArticleSource source =
        source(
            "Example News",
            "https://feeds.example.com/first.xml",
            "https://feeds.example.com/second.xml");
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.putInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY, 1);
    ArticleCollectionTargetReader reader =
        new ArticleCollectionTargetReader(articleSourceRepository);

    reader.open(executionContext);

    assertThat(reader.read())
        .isEqualTo(new ArticleCollectionTarget(source, "https://feeds.example.com/second.xml"));
    assertThat(reader.read()).isNull();
  }

  @Test
  void storesNextIndexOnUpdate() {
    ArticleSourceRepository articleSourceRepository = mock(ArticleSourceRepository.class);
    ArticleSource source =
        source(
            "Example News",
            "https://feeds.example.com/first.xml",
            "https://feeds.example.com/second.xml");
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    ExecutionContext executionContext = new ExecutionContext();
    ArticleCollectionTargetReader reader =
        new ArticleCollectionTargetReader(articleSourceRepository);

    reader.open(executionContext);
    reader.read();
    reader.update(executionContext);

    assertThat(executionContext.getInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY)).isEqualTo(1);
  }

  @Test
  void resetsNextIndex() {
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.putInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY, 2);

    ArticleCollectionTargetReader.resetSavedNextIndex(executionContext);

    assertThat(executionContext.getInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY)).isZero();
  }

  private ArticleSource source(String name, String... feedUrls) {
    return ArticleSource.create(
        name,
        new SourcePolicy(
            new CrawlingPolicy(
                List.of(feedUrls),
                List.of("news.example.com"),
                List.of("article"),
                List.of(),
                List.of())),
        "en",
        SourceType.RSS,
        LicenseInfo.createCcBy("4.0"));
  }
}

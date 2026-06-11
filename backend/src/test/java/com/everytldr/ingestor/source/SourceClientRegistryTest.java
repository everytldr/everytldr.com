package com.everytldr.ingestor.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceClientRegistryTest {

  private SourceClient getRssClient() {
    return new SourceClient() {
      @Override
      public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.RSS;
      }

      @Override
      public List<CollectedArticle> collect(ArticleSource source) {
        return List.of();
      }
    };
  }

  @Test
  void matchingClientIsReturned() {
    SourceClient rssClient = getRssClient();
    SourceClientRegistry sourceClientRegistry = new SourceClientRegistry(List.of(rssClient));

    SourceClient actual = sourceClientRegistry.getClient(SourceType.RSS);
    assertThat(actual).isSameAs(rssClient);
  }

  @Test
  void throwsWhenNoClientSupportsSourceType() {
    SourceClientRegistry sourceClientRegistry = new SourceClientRegistry(List.of());

    assertThatThrownBy(() -> sourceClientRegistry.getClient(SourceType.RSS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No SourceClient")
        .hasMessageContaining("RSS");
  }

  @Test
  void throwsWhenMultipleClientsSupportSourceType() {
    SourceClientRegistry sourceClientRegistry =
        new SourceClientRegistry(List.of(getRssClient(), getRssClient()));

    assertThatThrownBy(() -> sourceClientRegistry.getClient(SourceType.RSS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Multiple SourceClients")
        .hasMessageContaining("RSS");
  }
}

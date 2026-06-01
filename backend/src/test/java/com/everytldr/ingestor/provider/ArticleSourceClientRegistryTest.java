package com.everytldr.ingestor.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleSourceClientRegistryTest {

  private ArticleSourceClient getGuardianApiClient() {
    return new ArticleSourceClient() {
      @Override
      public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.GUARDIAN_API;
      }

      @Override
      public List<CollectedArticle> collect(ArticleSource source) {
        return List.of();
      }
    };
  }

  @Test
  void matchingClientIsReturned() {
    ArticleSourceClient guardianApiClient = getGuardianApiClient();
    ArticleSourceClientRegistry articleSourceClientRegistry =
        new ArticleSourceClientRegistry(List.of(guardianApiClient));

    ArticleSourceClient actual = articleSourceClientRegistry.getClient(SourceType.GUARDIAN_API);
    assertThat(actual).isSameAs(guardianApiClient);
  }

  @Test
  void throwsWhenNoClientSupportsSourceType() {
    ArticleSourceClientRegistry articleSourceClientRegistry =
        new ArticleSourceClientRegistry(List.of());

    assertThatThrownBy(() -> articleSourceClientRegistry.getClient(SourceType.RSS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No ArticleSourceClient")
        .hasMessageContaining("RSS");
  }

  @Test
  void throwsWhenMultipleClientsSupportSourceType() {
    ArticleSourceClientRegistry articleSourceClientRegistry =
        new ArticleSourceClientRegistry(List.of(getGuardianApiClient(), getGuardianApiClient()));

    assertThatThrownBy(() -> articleSourceClientRegistry.getClient(SourceType.GUARDIAN_API))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Multiple ArticleSourceClients")
        .hasMessageContaining("GUARDIAN_API");
  }
}

package com.everytldr.ingestor.provider.guardian;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.provider.CollectedArticle;
import org.junit.jupiter.api.Test;

class GuardianArticleMapperTest {

  private final GuardianArticleMapper guardianArticleMapper = new GuardianArticleMapper();

  @Test
  void mapsGuardianResultToCollectedArticle() {
    GuardianSearchResponse response =
        new GuardianSearchResponse(
            new GuardianSearchResponse.Response(
                List.of(
                    new GuardianSearchResponse.Result(
                        "2026-05-04T10:15:30Z",
                        "https://www.theguardian.com/football/example",
                        new GuardianSearchResponse.Fields(
                            "https://media.guim.co.uk/example-thumbnail.jpg")))));
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    List<CollectedArticle> actual = guardianArticleMapper.map(response, source);

    assertThat(actual).hasSize(1);
    CollectedArticle article = actual.getFirst();
    assertThat(article.sourceUrl()).isEqualTo("https://www.theguardian.com/football/example");
    assertThat(article.sourceName()).isEqualTo("The Guardian Football");
    assertThat(article.thumbnailUrl()).isEqualTo("https://media.guim.co.uk/example-thumbnail.jpg");
    assertThat(article.language()).isEqualTo("en");
    assertThat(article.publishedAt()).isEqualTo(Instant.parse("2026-05-04T10:15:30Z"));
  }

  @Test
  void mapsMissingThumbnailAsNull() {
    GuardianSearchResponse response =
        new GuardianSearchResponse(
            new GuardianSearchResponse.Response(
                List.of(
                    new GuardianSearchResponse.Result(
                        "2026-05-04T10:15:30Z",
                        "https://www.theguardian.com/football/no-thumbnail",
                        null))));
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    List<CollectedArticle> actual = guardianArticleMapper.map(response, source);

    assertThat(actual).hasSize(1);
    assertThat(actual.getFirst().thumbnailUrl()).isNull();
  }

  @Test
  void skipsInvalidGuardianResultsAndMapsValidOnes() {
    GuardianSearchResponse response =
        new GuardianSearchResponse(
            new GuardianSearchResponse.Response(
                List.of(
                    new GuardianSearchResponse.Result(
                        "not-a-date", "https://www.theguardian.com/football/invalid-date", null),
                    new GuardianSearchResponse.Result(
                        null, "https://www.theguardian.com/football/missing-date", null),
                    new GuardianSearchResponse.Result("2026-05-04T10:15:30Z", "", null),
                    new GuardianSearchResponse.Result(
                        "2026-05-04T10:15:30Z",
                        "https://www.theguardian.com/football/valid",
                        null))));
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    List<CollectedArticle> actual = guardianArticleMapper.map(response, source);

    assertThat(actual).hasSize(1);
    assertThat(actual.getFirst().sourceUrl())
        .isEqualTo("https://www.theguardian.com/football/valid");
    assertThat(actual.getFirst().publishedAt()).isEqualTo(Instant.parse("2026-05-04T10:15:30Z"));
  }

  @Test
  void returnsEmptyListWhenGuardianResponseIsMissingResults() {
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    assertThat(guardianArticleMapper.map(null, source)).isEmpty();
    assertThat(guardianArticleMapper.map(new GuardianSearchResponse(null), source)).isEmpty();
    assertThat(
            guardianArticleMapper.map(
                new GuardianSearchResponse(new GuardianSearchResponse.Response(null)), source))
        .isEmpty();
  }
}

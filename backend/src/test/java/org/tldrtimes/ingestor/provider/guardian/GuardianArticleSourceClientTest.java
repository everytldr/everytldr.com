package org.tldrtimes.ingestor.provider.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.tldrtimes.common.domain.source.ArticleSource;
import org.tldrtimes.common.domain.source.SourceType;
import org.tldrtimes.ingestor.provider.CollectedArticle;

class GuardianArticleSourceClientTest {

  @Test
  void supportsGuardianApiSourceTypeOnly() {
    GuardianArticleSourceClient client = newClient(defaultProperties(), RestClient.builder());

    assertThat(client.supports(SourceType.GUARDIAN_API)).isTrue();
    assertThat(client.supports(SourceType.RSS)).isFalse();
  }

  @Test
  void collectsArticlesFromGuardianSearchApi() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    GuardianArticleSourceClient.ClientProperties properties = defaultProperties();
    GuardianArticleSourceClient client = newClient(properties, restClientBuilder);
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    server
        .expect(requestTo(startsWith("https://content.guardianapis.com/search")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(queryParam("section", "football"))
        .andExpect(queryParam("order-by", "newest"))
        .andExpect(queryParam("show-fields", "thumbnail"))
        .andExpect(queryParam("page-size", "2"))
        .andExpect(queryParam("api-key", "test-key"))
        .andRespond(withSuccess(searchResponseJson(), MediaType.APPLICATION_JSON));

    List<CollectedArticle> actual = client.collect(source);

    assertThat(actual).hasSize(1);
    CollectedArticle article = actual.getFirst();
    assertThat(article.sourceUrl()).isEqualTo("https://www.theguardian.com/football/example");
    assertThat(article.sourceName()).isEqualTo("The Guardian Football");
    assertThat(article.thumbnailUrl()).isEqualTo("https://media.guim.co.uk/example-thumbnail.jpg");
    assertThat(article.language()).isEqualTo("en");
    assertThat(article.publishedAt()).isEqualTo(Instant.parse("2026-05-07T10:15:30Z"));
    server.verify();
  }

  @Test
  void returnsEmptyListWhenGuardianResultsAreEmpty() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    GuardianArticleSourceClient client = newClient(defaultProperties(), restClientBuilder);
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    server
        .expect(requestTo(startsWith("https://content.guardianapis.com/search")))
        .andRespond(withSuccess("{\"response\":{\"results\":[]}}", MediaType.APPLICATION_JSON));

    assertThat(client.collect(source)).isEmpty();
    server.verify();
  }

  @Test
  void throwsWhenApiKeyIsMissing() {
    GuardianArticleSourceClient.ClientProperties properties =
        new GuardianArticleSourceClient.ClientProperties("https://content.guardianapis.com", "", 2);
    GuardianArticleSourceClient client = newClient(properties, RestClient.builder());
    ArticleSource source =
        ArticleSource.create(
            "The Guardian Football",
            "https://content.guardianapis.com/search?section=football",
            "en",
            SourceType.GUARDIAN_API);

    assertThatThrownBy(() -> client.collect(source))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Guardian API key");
  }

  @Test
  void throwsWhenSourceLocatorHasNoSection() {
    GuardianArticleSourceClient client = newClient(defaultProperties(), RestClient.builder());
    ArticleSource source =
        ArticleSource.create(
            "The Guardian",
            "https://content.guardianapis.com/search",
            "en",
            SourceType.GUARDIAN_API);

    assertThatThrownBy(() -> client.collect(source))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("section");
  }

  private GuardianArticleSourceClient newClient(
      GuardianArticleSourceClient.ClientProperties properties, RestClient.Builder restClientBuilder) {
    return new GuardianArticleSourceClient(restClientBuilder, properties, new GuardianArticleMapper());
  }

  private GuardianArticleSourceClient.ClientProperties defaultProperties() {
    return new GuardianArticleSourceClient.ClientProperties("https://content.guardianapis.com", "test-key", 2);
  }

  private String searchResponseJson() {
    return """
        {
          "response": {
            "results": [
              {
                "webPublicationDate": "2026-05-07T10:15:30Z",
                "webUrl": "https://www.theguardian.com/football/example",
                "fields": {
                  "thumbnail": "https://media.guim.co.uk/example-thumbnail.jpg"
                }
              }
            ]
          }
        }
        """;
  }
}

package com.everytldr.ingestor.source.rss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.source.CollectedArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RssSourceClientTest {

  private static final String RSS_URL = "https://news.example.com/rss.xml";

  @Test
  void supportsRssSourceType() {
    assertThat(newClient(RestClient.builder()).supports(SourceType.RSS)).isTrue();
  }

  @Test
  void collectsValidFeedEntries() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = server(restClientBuilder, feedWithOneArticle());

    List<CollectedArticle> articles = newClient(restClientBuilder).collect(source());

    assertThat(articles)
        .containsExactly(
            new CollectedArticle(
                "https://news.example.com/article",
                "Example News",
                null,
                "en",
                Instant.parse("2026-05-08T08:25:43Z")));
    server.verify();
  }

  @Test
  void resolvesThumbnailFromFeedMedia() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    server(restClientBuilder, feedWithMedia());

    List<CollectedArticle> articles = newClient(restClientBuilder).collect(source());

    assertThat(articles)
        .extracting(CollectedArticle::thumbnailUrl)
        .containsExactly(
            "https://cdn.example.com/thumb.jpg", "https://cdn.example.com/content.webp");
  }

  @Test
  void skipsEntriesMissingRequiredFields() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    server(restClientBuilder, feedWithInvalidEntries());

    assertThat(newClient(restClientBuilder).collect(source())).isEmpty();
  }

  @Test
  void rejectsInvalidFeedXml() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    server(restClientBuilder, "<rss><channel>");

    assertThatThrownBy(() -> newClient(restClientBuilder).collect(source()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to parse RSS feed")
        .hasMessageContaining("Example News");
  }

  private RssSourceClient newClient(RestClient.Builder restClientBuilder) {
    return new RssSourceClient(restClientBuilder);
  }

  private MockRestServiceServer server(RestClient.Builder restClientBuilder, String body) {
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    server
        .expect(requestTo(startsWith(RSS_URL)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(body, MediaType.APPLICATION_XML));
    return server;
  }

  private ArticleSource source() {
    return ArticleSource.create(
        "Example News",
        RSS_URL,
        new SourcePolicy(
            new CrawlingPolicy(List.of("news.example.com"), List.of("article"), List.of())),
        "en",
        SourceType.RSS);
  }

  private String feedWithOneArticle() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <item>
              <link>https://news.example.com/article</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """;
  }

  private String feedWithMedia() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
          <channel>
            <item>
              <link>https://news.example.com/thumbnail</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
              <media:thumbnail url="https://cdn.example.com/thumb.jpg"/>
              <media:content url="https://cdn.example.com/other.webp" medium="image"/>
            </item>
            <item>
              <link>https://news.example.com/content</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
              <media:content url="https://cdn.example.com/content.webp" medium="image"/>
            </item>
          </channel>
        </rss>
        """;
  }

  private String feedWithInvalidEntries() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <item>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>https://news.example.com/missing-date</link>
            </item>
          </channel>
        </rss>
        """;
  }
}

package com.everytldr.ingestor.provider.rss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.provider.CollectedArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RssArticleSourceClientTest {

  private static final String BBC_FOOTBALL_RSS_URL =
      "http://newsrss.bbc.co.uk/rss/sportonline_uk_edition/football/rss.xml";

  @Test
  void supportsRssSourceTypeOnly() {
    RssArticleSourceClient client = newClient(RestClient.builder());

    assertThat(client.supports(SourceType.RSS)).isTrue();
    assertThat(client.supports(SourceType.GUARDIAN_API)).isFalse();
  }

  @Test
  void collectsArticlesFromRssFeed() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    RssArticleSourceClient client = newClient(restClientBuilder);
    ArticleSource source = bbcFootballSource();

    server
        .expect(requestTo(startsWith(BBC_FOOTBALL_RSS_URL)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(rssFeed(), MediaType.APPLICATION_XML));

    List<CollectedArticle> actual = client.collect(source);

    assertThat(actual).hasSize(1);
    CollectedArticle article = actual.getFirst();
    assertThat(article.sourceUrl()).isEqualTo("https://www.bbc.com/sport/football/example");
    assertThat(article.sourceName()).isEqualTo("BBC Sport");
    assertThat(article.thumbnailUrl()).isNull();
    assertThat(article.language()).isEqualTo("en");
    assertThat(article.publishedAt()).isEqualTo(Instant.parse("2026-05-08T08:25:43Z"));
    server.verify();
  }

  @Test
  void extractsThumbnailFromMediaContentImage() {
    assertThat(findArticle("https://example.com/media-content").thumbnailUrl())
        .isEqualTo("https://cdn.example.com/content.webp");
  }

  @Test
  void prefersMediaThumbnailOverMediaContent() {
    assertThat(findArticle("https://example.com/media-thumbnail").thumbnailUrl())
        .isEqualTo("https://cdn.example.com/thumb.jpg");
  }

  @Test
  void ignoresNonImageMediaContent() {
    assertThat(findArticle("https://example.com/video").thumbnailUrl()).isNull();
  }

  @Test
  void extractsThumbnailFromImageEnclosure() {
    assertThat(findArticle("https://example.com/enclosure").thumbnailUrl())
        .isEqualTo("https://cdn.example.com/enclosure.jpg");
  }

  @Test
  void skipsEntriesWithoutLinkOrPublishedDate() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    RssArticleSourceClient client = newClient(restClientBuilder);

    server
        .expect(requestTo(startsWith(BBC_FOOTBALL_RSS_URL)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(rssFeedWithInvalidItems(), MediaType.APPLICATION_XML));

    List<CollectedArticle> actual = client.collect(bbcFootballSource());

    assertThat(actual).isEmpty();
    server.verify();
  }

  @Test
  void returnsEmptyListWhenFeedHasNoEntries() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    RssArticleSourceClient client = newClient(restClientBuilder);

    server
        .expect(requestTo(startsWith(BBC_FOOTBALL_RSS_URL)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(emptyRssFeed(), MediaType.APPLICATION_XML));

    assertThat(client.collect(bbcFootballSource())).isEmpty();
    server.verify();
  }

  @Test
  void throwsWhenFeedXmlIsInvalid() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    RssArticleSourceClient client = newClient(restClientBuilder);

    server
        .expect(requestTo(startsWith(BBC_FOOTBALL_RSS_URL)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("<rss><channel>", MediaType.APPLICATION_XML));

    assertThatThrownBy(() -> client.collect(bbcFootballSource()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to parse RSS feed")
        .hasMessageContaining("BBC Sport");
    server.verify();
  }

  @Test
  void throwsWhenFeedResponseIsEmpty() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    RssArticleSourceClient client = newClient(restClientBuilder);

    server
        .expect(requestTo(startsWith(BBC_FOOTBALL_RSS_URL)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("", MediaType.APPLICATION_XML));

    assertThatThrownBy(() -> client.collect(bbcFootballSource()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RSS feed response is empty")
        .hasMessageContaining("BBC Sport");
    server.verify();
  }

  private RssArticleSourceClient newClient(RestClient.Builder restClientBuilder) {
    return new RssArticleSourceClient(restClientBuilder);
  }

  private ArticleSource bbcFootballSource() {
    return ArticleSource.create("BBC Sport", BBC_FOOTBALL_RSS_URL, "en", SourceType.RSS);
  }

  private String rssFeed() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>BBC Sport - Football</title>
            <item>
              <title>Example football news</title>
              <link>https://www.bbc.com/sport/football/example</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """;
  }

  private CollectedArticle findArticle(String link) {
    String feedUrl = "https://feed.example.com/rss";
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer.bindTo(restClientBuilder)
        .build()
        .expect(requestTo(startsWith(feedUrl)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(rssFeedWithImages(), MediaType.APPLICATION_XML));

    ArticleSource source = ArticleSource.create("Example", feedUrl, "en", SourceType.RSS);
    return newClient(restClientBuilder).collect(source).stream()
        .filter(article -> article.sourceUrl().equals(link))
        .findFirst()
        .orElseThrow();
  }

  private String rssFeedWithImages() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
          <channel>
            <title>Example Feed</title>
            <item>
              <link>https://example.com/media-content</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
              <media:content url="https://cdn.example.com/content.webp" medium="image"/>
            </item>
            <item>
              <link>https://example.com/media-thumbnail</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
              <media:thumbnail url="https://cdn.example.com/thumb.jpg"/>
              <media:content url="https://cdn.example.com/other.webp" medium="image"/>
            </item>
            <item>
              <link>https://example.com/enclosure</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
              <enclosure url="https://cdn.example.com/enclosure.jpg" type="image/jpeg"/>
            </item>
            <item>
              <link>https://example.com/video</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
              <media:content url="https://cdn.example.com/clip.mp4" medium="video" type="video/mp4"/>
            </item>
          </channel>
        </rss>
        """;
  }

  private String rssFeedWithInvalidItems() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>BBC Sport - Football</title>
            <item>
              <title>Missing link</title>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <title>Missing date</title>
              <link>https://www.bbc.com/sport/football/missing-date</link>
            </item>
          </channel>
        </rss>
        """;
  }

  private String emptyRssFeed() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>BBC Sport - Football</title>
          </channel>
        </rss>
        """;
  }
}

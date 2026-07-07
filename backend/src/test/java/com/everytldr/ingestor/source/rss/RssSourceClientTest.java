package com.everytldr.ingestor.source.rss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.ingestion.IngestionExceptions;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import com.everytldr.ingestor.source.CollectedArticle;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RssSourceClientTest {

  private HttpServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void supportsRssSourceType() {
    assertThat(newClient().supports(SourceType.RSS)).isTrue();
  }

  @Test
  void collectsValidSingleFeedTarget() {
    route("/rss.xml", 200, feedWithOneArticle());
    ArticleSource source = source("/rss.xml");

    List<CollectedArticle> articles = newClient().collect(firstTarget(source));

    assertThat(articles)
        .containsExactly(
            new CollectedArticle(
                "https://news.example.com/article",
                "Example News",
                null,
                "en",
                Instant.parse("2026-05-08T08:25:43Z"),
                licenseInfo()));
  }

  @Test
  void throwsRetryableWhenSingleFeedHttpRequestFails() {
    route("/broken.xml", 500, "boom");
    ArticleSource source = source("/broken.xml");

    assertThatThrownBy(() -> newClient().collect(firstTarget(source)))
        .isInstanceOf(IngestionExceptions.Retryable.class)
        .hasMessageContaining("Failed to fetch RSS feed");
  }

  @Test
  void throwsSkippableWhenSingleFeedXmlIsInvalid() {
    route("/broken.xml", 200, "<rss><channel>");
    ArticleSource source = source("/broken.xml");

    assertThatThrownBy(() -> newClient().collect(firstTarget(source)))
        .isInstanceOf(IngestionExceptions.Skippable.class)
        .hasMessageContaining("Failed to read RSS feed");
  }

  @Test
  void throwsSkippableWhenSingleFeedBodyIsEmpty() {
    route("/empty.xml", 200, " ");
    ArticleSource source = source("/empty.xml");

    assertThatThrownBy(() -> newClient().collect(firstTarget(source)))
        .isInstanceOf(IngestionExceptions.Skippable.class)
        .hasMessageContaining("Failed to read RSS feed");
  }

  @Test
  void resolvesThumbnailFromFeedMedia() {
    route("/rss.xml", 200, feedWithMedia());
    ArticleSource source = source("/rss.xml");

    List<CollectedArticle> articles = newClient().collect(firstTarget(source));

    assertThat(articles)
        .extracting(CollectedArticle::thumbnailUrl)
        .containsExactly(
            "https://cdn.example.com/thumb.jpg", "https://cdn.example.com/content.webp");
  }

  @Test
  void skipsEntriesMissingRequiredFields() {
    route("/rss.xml", 200, feedWithInvalidEntries());
    ArticleSource source = source("/rss.xml");

    assertThat(newClient().collect(firstTarget(source))).isEmpty();
  }

  @Test
  void skipsEntriesOutsideAllowedContentHosts() {
    route("/rss.xml", 200, feedWithMixedAllowedAndBlockedLinks());
    ArticleSource source = source("/rss.xml");

    List<CollectedArticle> articles = newClient().collect(firstTarget(source));

    assertThat(articles)
        .extracting(CollectedArticle::contentUrl)
        .containsExactly("https://news.example.com/allowed");
  }

  @Test
  void skipsEntriesOutsideAllowedContentPathPrefixes() {
    route("/rss.xml", 200, feedWithMixedAllowedAndBlockedPaths());

    List<CollectedArticle> articles =
        newClient()
            .collect(
                firstTarget(
                    sourceWithAllowedPathPrefixes(
                        List.of("/global/news/", "/global/features/"), "/rss.xml")));

    assertThat(articles)
        .extracting(CollectedArticle::contentUrl)
        .containsExactly(
            "https://news.example.com/global/news/allowed",
            "https://news.example.com/global/features/allowed");
  }

  private RssSourceClient newClient() {
    return new RssSourceClient(
        RestClient.builder(), new FeedProperties(Duration.ofSeconds(2), Duration.ofSeconds(2)));
  }

  private ArticleSource source(String... paths) {
    return sourceWithAllowedPathPrefixes(List.of(), paths);
  }

  private ArticleCollectionTarget firstTarget(ArticleSource source) {
    return new ArticleCollectionTarget(source, source.getPolicy().crawling().feedUrls().getFirst());
  }

  private ArticleSource sourceWithAllowedPathPrefixes(
      List<String> allowedPathPrefixes, String... paths) {
    List<String> feedUrls = Arrays.stream(paths).map(path -> serverUrl() + path).toList();
    return ArticleSource.create(
        "Example News",
        new SourcePolicy(
            new CrawlingPolicy(
                feedUrls,
                List.of("news.example.com"),
                List.of("article"),
                List.of(),
                allowedPathPrefixes)),
        "en",
        SourceType.RSS,
        licenseInfo());
  }

  private LicenseInfo licenseInfo() {
    return LicenseInfo.createCcBy("4.0");
  }

  private String serverUrl() {
    return "http://localhost:%d".formatted(server.getAddress().getPort());
  }

  private void route(String path, int status, String body) {
    server.createContext(
        path,
        exchange -> {
          byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/xml");
          exchange.sendResponseHeaders(status, responseBody.length);
          try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
          }
        });
  }

  private String feedWithArticle(String link) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <item>
              <link>%s</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """
        .formatted(link);
  }

  private String feedWithOneArticle() {
    return feedWithArticle("https://news.example.com/article");
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

  private String feedWithMixedAllowedAndBlockedLinks() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <item>
              <link>https://news.example.com/allowed</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>https://partner.example.com/external</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>not a url</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>/relative</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>ftp://news.example.com/file</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """;
  }

  private String feedWithMixedAllowedAndBlockedPaths() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <item>
              <link>https://news.example.com/global/news/allowed</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>https://news.example.com/global/features/allowed</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>https://news.example.com/global/podcast/blocked</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
            <item>
              <link>https://news.example.com/global/supported-content/blocked</link>
              <pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """;
  }
}

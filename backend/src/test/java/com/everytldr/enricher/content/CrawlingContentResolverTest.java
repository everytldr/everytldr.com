package com.everytldr.enricher.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.enricher.content.ContentResolver.ResolvedArticle;
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrawlingContentResolverTest {
  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");

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
  void supportsOnlyArticlesFromAllowedSourceHosts() {
    CrawlingContentResolver resolver = resolver(List.of("globalvoices.org"), List.of("article"));

    assertThat(resolver.supports(article("https://globalvoices.org/story"))).isTrue();
    assertThat(resolver.supports(article("https://example.com/story"))).isFalse();
    assertThat(resolver.supports(article("ftp://globalvoices.org/story"))).isFalse();
    assertThat(resolver.supports(article("not a url"))).isFalse();
  }

  @Test
  void extractsArticleBodyWithConfiguredSelectors() {
    route(
        "/story",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        """
        <html>
          <body>
            <main>Main fallback should not be used.</main>
            <article>
              <h1>Match report</h1>
              <p>%s</p>
              <script>tracking()</script>
            </article>
          </body>
        </html>
        """
            .formatted("Tottenham controlled the first half. ".repeat(5)));

    String content =
        resolver(List.of("localhost"), List.of("article"))
            .resolve(article(serverUrl("/story")))
            .content();

    assertThat(content).contains("Match report").contains("Tottenham controlled");
    assertThat(content).doesNotContain("tracking").doesNotContain("Main fallback");
  }

  @Test
  void resolvesThumbnailFromOpenGraphImageWhenArticleHasNone() {
    route(
        "/story",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        """
        <html>
          <head>
            <meta property="og:image" content="https://cdn.example.com/hero.jpg" />
          </head>
          <body>
            <article><p>%s</p></article>
          </body>
        </html>
        """
            .formatted("Tottenham controlled the first half. ".repeat(5)));

    ResolvedArticle resolved =
        resolver(List.of("localhost"), List.of("article")).resolve(article(serverUrl("/story")));

    assertThat(resolved.thumbnailUrl()).isEqualTo("https://cdn.example.com/hero.jpg");
  }

  @Test
  void resolvesThumbnailFromConfiguredSelectorsWhenOpenGraphMissing() {
    route(
        "/story",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        """
        <html>
          <body>
            <header><img src="https://cdn.example.com/logo.png" /></header>
            <figure class="feat-image"><img src="https://cdn.example.com/feature.jpg" /></figure>
            <article><p>%s</p></article>
          </body>
        </html>
        """
            .formatted("Tottenham controlled the first half. ".repeat(5)));

    ResolvedArticle resolved =
        resolver(List.of("localhost"), List.of("article"), List.of(".feat-image > img"))
            .resolve(article(serverUrl("/story")));

    assertThat(resolved.thumbnailUrl()).isEqualTo("https://cdn.example.com/feature.jpg");
  }

  @Test
  void prefersConfiguredThumbnailSelectorOverOpenGraphImage() {
    route(
        "/story",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        """
        <html>
          <head>
            <meta property="og:image" content="https://cdn.example.com/og.jpg" />
          </head>
          <body>
            <figure class="feat-image"><img src="https://cdn.example.com/feature.jpg" /></figure>
            <article><p>%s</p></article>
          </body>
        </html>
        """
            .formatted("Tottenham controlled the first half. ".repeat(5)));

    ResolvedArticle resolved =
        resolver(List.of("localhost"), List.of("article"), List.of(".feat-image > img"))
            .resolve(article(serverUrl("/story")));

    assertThat(resolved.thumbnailUrl()).isEqualTo("https://cdn.example.com/feature.jpg");
  }

  @Test
  void rejectsDisallowedInitialUriAsPermanentFailure() {
    route(
        "/story",
        200,
        Map.of("Content-Type", "text/html"),
        "<html><body><article>%s</article></body></html>".formatted("content ".repeat(10)));

    EnrichmentException exception =
        catchThrowableOfType(
            () ->
                resolver(List.of("globalvoices.org"), List.of("article"))
                    .resolve(article(serverUrl("/story"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("crawled request URI is not allowed");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsTemporaryHttpFailuresAsRetryable() {
    route("/unavailable", 503, Map.of("Content-Type", "text/html"), "temporarily unavailable");

    EnrichmentException exception =
        catchThrowableOfType(
            () ->
                resolver(List.of("localhost"), List.of("article"))
                    .resolve(article(serverUrl("/unavailable"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("retryable article content response status: 503");
    assertThat(exception.isRetryable()).isTrue();
  }

  @Test
  void rejectsInitialUriBeforeSendingRequest() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext("/blocked", exchange -> requestCount.incrementAndGet());

    EnrichmentException exception =
        catchThrowableOfType(
            () ->
                new ContentCrawler(Duration.ofSeconds(2), 4096)
                    .crawl(URI.create(serverUrl("/blocked")), ignored -> false),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("crawled request URI is not allowed");
    assertThat(exception.isRetryable()).isFalse();
    assertThat(requestCount).hasValue(0);
  }

  @Test
  void followsAllowedRedirectAndUsesFinalUriForRelativeThumbnailUrl() {
    route("/redirect", 302, Map.of("Location", "/articles/final/"), "");
    route(
        "/articles/final/",
        200,
        Map.of("Content-Type", "text/html"),
        """
        <html>
          <head><meta property="og:image" content="thumbnail.jpg" /></head>
          <body><article>%s</article></body>
        </html>
        """
            .formatted("content ".repeat(10)));

    ResolvedArticle resolved =
        resolver(List.of("localhost"), List.of("article")).resolve(article(serverUrl("/redirect")));

    assertThat(resolved.content()).contains("content");
    assertThat(resolved.thumbnailUrl()).isEqualTo(serverUrl("/articles/final/thumbnail.jpg"));
  }

  @Test
  void rejectsDisallowedRedirectBeforeSendingRedirectRequest() {
    AtomicInteger blockedRequestCount = new AtomicInteger();
    server.createContext("/blocked", exchange -> blockedRequestCount.incrementAndGet());
    route(
        "/redirect",
        302,
        Map.of("Location", "http://127.0.0.1:%d/blocked".formatted(server.getAddress().getPort())),
        "");

    EnrichmentException exception =
        catchThrowableOfType(
            () ->
                resolver(List.of("localhost"), List.of("article"))
                    .resolve(article(serverUrl("/redirect"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("crawled request URI is not allowed");
    assertThat(exception.isRetryable()).isFalse();
    assertThat(blockedRequestCount).hasValue(0);
  }

  @Test
  void rejectsRedirectChainsLongerThanThreeHops() {
    AtomicInteger fifthRequestCount = new AtomicInteger();
    route("/one", 302, Map.of("Location", "/two"), "");
    route("/two", 302, Map.of("Location", "/three"), "");
    route("/three", 302, Map.of("Location", "/four"), "");
    route("/four", 302, Map.of("Location", "/five"), "");
    server.createContext("/five", exchange -> fifthRequestCount.incrementAndGet());

    EnrichmentException exception =
        catchThrowableOfType(
            () ->
                resolver(List.of("localhost"), List.of("article"))
                    .resolve(article(serverUrl("/one"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("exceeded redirect limit");
    assertThat(exception.isRetryable()).isFalse();
    assertThat(fifthRequestCount).hasValue(0);
  }

  @Test
  void rejectsUnusableExtractedBodyAsPermanentFailure() {
    route(
        "/short",
        200,
        Map.of("Content-Type", "text/html"),
        "<html><body><article>short</article></body></html>");

    EnrichmentException exception =
        catchThrowableOfType(
            () ->
                resolver(List.of("localhost"), List.of("article"))
                    .resolve(article(serverUrl("/short"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("extracted article body is too short");
    assertThat(exception.isRetryable()).isFalse();
  }

  private CrawlingContentResolver resolver(List<String> hosts, List<String> contentSelectors) {
    return resolver(hosts, contentSelectors, List.of());
  }

  private CrawlingContentResolver resolver(
      List<String> hosts, List<String> contentSelectors, List<String> thumbnailSelectors) {
    ArticleSourceProvider sourceProvider = mock(ArticleSourceProvider.class);
    when(sourceProvider.findByName("Global Voices"))
        .thenReturn(Optional.of(source(hosts, contentSelectors, thumbnailSelectors)));

    return new CrawlingContentResolver(
        sourceProvider, new ContentCrawler(Duration.ofSeconds(2), 4096), 20);
  }

  private ArticleSource source(
      List<String> hosts, List<String> contentSelectors, List<String> thumbnailSelectors) {
    return ArticleSource.create(
        "Global Voices",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of("https://globalvoices.org/feed/"),
                hosts,
                contentSelectors,
                thumbnailSelectors,
                List.of())),
        "en",
        SourceType.RSS,
        LicenseInfo.createCcBy("4.0"));
  }

  private Article article(String contentUrl) {
    return Article.create(contentUrl, "Global Voices", null, "en", PUBLISHED_AT);
  }

  private String serverUrl(String path) {
    return "http://localhost:%d%s".formatted(server.getAddress().getPort(), path);
  }

  private void route(String path, int status, Map<String, String> headers, String body) {
    server.createContext(
        path,
        exchange -> {
          headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
          byte[] responseBody = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, responseBody.length);
          try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
          }
        });
  }
}

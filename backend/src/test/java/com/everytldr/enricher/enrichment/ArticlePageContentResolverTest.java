package com.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.everytldr.common.domain.article.Article;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArticlePageContentResolverTest {
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
  void supportsAllowlistedHttpAndHttpsUrlsOnly() {
    ArticlePageContentResolver resolver =
        createResolver(List.of("globalvoices.org", "www.globalvoices.org"), 3, 1024, 20);

    assertThat(resolver.supports(createArticle("https://globalvoices.org/example"))).isTrue();
    assertThat(resolver.supports(createArticle("http://www.globalvoices.org/example"))).isTrue();
    assertThat(resolver.supports(createArticle("https://unsupported.example.com/example")))
        .isFalse();
    assertThat(resolver.supports(createArticle("file:///etc/passwd"))).isFalse();
    assertThat(resolver.supports(createArticle("not a url"))).isFalse();
  }

  @Test
  void resolvesAllowlistedHtmlArticleBodyAndKeepsArticleMetadata() {
    String bodyText =
        "Tottenham controlled the first half before the match shifted after the interval. "
            .repeat(5);
    route(
        "/article",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        """
        <html>
          <head>
            <style>.hidden { display: none; }</style>
            <script>console.log("tracking");</script>
          </head>
          <body>
            <main>Main fallback should not be used.</main>
            <article>
              <h1>Match report</h1>
              <p>%s</p>
              <noscript>tracking pixel</noscript>
            </article>
          </body>
        </html>
        """
            .formatted(bodyText));
    String sourceUrl = serverUrl("/article");
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 40);

    ArticleContent content = resolver.resolve(createArticle(sourceUrl));

    assertThat(content.sourceUrl()).isEqualTo(sourceUrl);
    assertThat(content.source()).isEqualTo("Global Voices");
    assertThat(content.language()).isEqualTo("en");
    assertThat(content.body()).contains("Match report").contains("Tottenham controlled");
    assertThat(content.body()).doesNotContain("tracking").doesNotContain("Main fallback");
  }

  @Test
  void followsAllowedRedirects() {
    route("/start", 302, Map.of("Location", "/final"), "");
    route(
        "/final",
        200,
        Map.of("Content-Type", "text/html"),
        "<html><body><article>%s</article></body></html>"
            .formatted("Final article text. ".repeat(20)));
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 40);

    ArticleContent content = resolver.resolve(createArticle(serverUrl("/start")));

    assertThat(content.body()).contains("Final article text");
  }

  @Test
  void rejectsDisallowedRedirectTargetAsPermanentFailure() {
    route("/start", 302, Map.of("Location", "https://evil.example.com/final"), "");
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 40);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/start"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("redirect target is not allowed");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void rejectsRedirectLimitExceededAsPermanentFailure() {
    route("/one", 302, Map.of("Location", "/two"), "");
    route("/two", 302, Map.of("Location", "/final"), "");
    route(
        "/final",
        200,
        Map.of("Content-Type", "text/html"),
        "<html><body><article>%s</article></body></html>".formatted("Too late. ".repeat(20)));
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 1, 4096, 40);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/one"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("redirect limit exceeded");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void treatsServerFailureAsRetryable() {
    route("/unavailable", 503, Map.of("Content-Type", "text/html"), "temporarily unavailable");
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 40);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/unavailable"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("retryable article content response status: 503");
    assertThat(exception.isRetryable()).isTrue();
  }

  @Test
  void rejectsClientFailureAsPermanentFailure() {
    route("/missing", 404, Map.of("Content-Type", "text/html"), "missing");
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 40);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/missing"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("non-success article content response status: 404");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void rejectsTooLargeBodyAsPermanentFailure() {
    route(
        "/too-large",
        200,
        Map.of("Content-Type", "text/html"),
        "<html><body><article>%s</article></body></html>".formatted("large body ".repeat(30)));
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 64, 20);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/too-large"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("article content response is too large");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void rejectsNonHtmlBodyAsPermanentFailure() {
    route("/json", 200, Map.of("Content-Type", "application/json"), "{}");
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 20);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/json"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("non-HTML article content response");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void rejectsShortExtractedBodyAsPermanentFailure() {
    route(
        "/short",
        200,
        Map.of("Content-Type", "text/html"),
        "<html><body><article>short</article></body></html>");
    ArticlePageContentResolver resolver = createResolver(List.of("localhost"), 3, 4096, 20);

    ArticleEnrichmentException exception =
        catchThrowableOfType(
            () -> resolver.resolve(createArticle(serverUrl("/short"))),
            ArticleEnrichmentException.class);

    assertThat(exception).hasMessageContaining("extracted article body is too short");
    assertThat(exception.isRetryable()).isFalse();
  }

  private ArticlePageContentResolver createResolver(
      List<String> allowedHosts, int maxRedirects, int maxBodyBytes, int minBodyChars) {
    return new ArticlePageContentResolver(
        new EnricherContentProperties(
            allowedHosts, Duration.ofSeconds(2), maxRedirects, maxBodyBytes, minBodyChars));
  }

  private Article createArticle(String sourceUrl) {
    return Article.create(sourceUrl, "Global Voices", null, "en", PUBLISHED_AT);
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

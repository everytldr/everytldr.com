package com.everytldr.enricher.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.ArticleEligibilityRule;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourcePolicy.EligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.RuleType;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailCandidateSelector;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailEligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.enricher.content.ContentResolver.ResolvedArticle;
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
  void allowsVoaArticleWithIndividualAuthorByline() {
    route(
        "/voa-author",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link" href="/author/elizabeth-lee/__qqy">Elizabeth Lee</a>
            </li>
            """,
            "Texas communities preserve local history. ".repeat(5),
            """
            <meta property="og:image" content="https://cdn.example.com/voa.jpg" />
            """));

    ResolvedArticle resolved = voaResolver().resolve(voaArticle(serverUrl("/voa-author")));

    assertThat(resolved.content()).contains("Texas communities preserve local history");
    assertThat(resolved.thumbnailUrl()).isNull();
  }

  @Test
  void allowsVoaArticleWithAbsoluteAuthorBylineHref() {
    route(
        "/voa-author-absolute",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link"
                    href="https://www.voanews.com/author/elizabeth-lee/__qqy">Elizabeth Lee</a>
            </li>
            """,
            "Texas communities preserve local history. ".repeat(5),
            ""));

    ResolvedArticle resolved = voaResolver().resolve(voaArticle(serverUrl("/voa-author-absolute")));

    assertThat(resolved.content()).contains("Texas communities preserve local history");
    assertThat(resolved.thumbnailUrl()).isNull();
  }

  @Test
  void resolvesEligibleOnlyThumbnailWhenCreditIsAllowed() {
    route(
        "/voa-thumbnail-allowed",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link" href="/author/elizabeth-lee/__qqy">Elizabeth Lee</a>
            </li>
            """,
            "Texas communities preserve local history. ".repeat(5),
            "",
            """
            <figure>
              <img src="/images/voa.jpg" />
              <figcaption>Photo: Voice of America</figcaption>
            </figure>
            """));

    ResolvedArticle resolved =
        voaResolver().resolve(voaArticle(serverUrl("/voa-thumbnail-allowed")));

    assertThat(resolved.content()).contains("Texas communities preserve local history");
    assertThat(resolved.thumbnailUrl()).isEqualTo(serverUrl("/images/voa.jpg"));
  }

  @Test
  void skipsEligibleOnlyThumbnailWhenCreditIsDenied() {
    route(
        "/voa-thumbnail-denied",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link" href="/author/elizabeth-lee/__qqy">Elizabeth Lee</a>
            </li>
            """,
            "Texas communities preserve local history. ".repeat(5),
            "",
            """
            <figure>
              <img src="/images/voa.jpg" />
              <figcaption>Photo: Getty</figcaption>
            </figure>
            """));

    ResolvedArticle resolved =
        voaResolver().resolve(voaArticle(serverUrl("/voa-thumbnail-denied")));

    assertThat(resolved.content()).contains("Texas communities preserve local history");
    assertThat(resolved.thumbnailUrl()).isNull();
  }

  @Test
  void skipsEligibleOnlyThumbnailWhenCreditIsMissing() {
    route(
        "/voa-thumbnail-missing-credit",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link" href="/author/elizabeth-lee/__qqy">Elizabeth Lee</a>
            </li>
            """,
            "Texas communities preserve local history. ".repeat(5),
            "",
            """
            <figure>
              <img src="/images/voa.jpg" />
            </figure>
            """));

    ResolvedArticle resolved =
        voaResolver().resolve(voaArticle(serverUrl("/voa-thumbnail-missing-credit")));

    assertThat(resolved.content()).contains("Texas communities preserve local history");
    assertThat(resolved.thumbnailUrl()).isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "VOA News",
        "AP",
        "AP News",
        "AFP",
        "Reuters",
        "Associated Press",
        "Agence France-Presse",
        "Agence France Presse"
      })
  void rejectsVoaDeniedBylineAsPermanentFailure(String byline) {
    route(
        "/voa-news",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link" href="/author/voa-news/oumqq">%s</a>
            </li>
            """
                .formatted(byline),
            "Leaders discussed regional security. ".repeat(5),
            ""));

    EnrichmentException exception =
        catchThrowableOfType(
            () -> voaResolver().resolve(voaArticle(serverUrl("/voa-news"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("article failed source eligibility policy");
    assertThat(exception).hasMessageContaining("selector text matched denied value: " + byline);
    assertThat(exception.isRetryable()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "wire service reports",
        "Associated Press",
        "AP Photo",
        "AP News",
        "Reuters",
        "Agence France-Presse",
        "Agence France Presse",
        "(AFP)"
      })
  void rejectsVoaArticleWithDeniedTextFragments(String deniedTextFragment) {
    route(
        "/voa-denied",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml(
            """
            <li class="links__item">
              By <a class="links__item-link" href="/author/elizabeth-lee/__qqy">Elizabeth Lee</a>
            </li>
            """,
            "Regional officials released a statement. ".repeat(5),
            "<script type=\"application/ld+json\">{\"credit\":\"%s\"}</script>"
                .formatted(deniedTextFragment)));

    EnrichmentException exception =
        catchThrowableOfType(
            () -> voaResolver().resolve(voaArticle(serverUrl("/voa-denied"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("article failed source eligibility policy");
    assertThat(exception)
        .hasMessageContaining("document html contains denied value: " + deniedTextFragment);
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void rejectsVoaArticleMissingRequiredByline() {
    route(
        "/voa-no-byline",
        200,
        Map.of("Content-Type", "text/html; charset=UTF-8"),
        voaArticleHtml("", "Regional officials released a statement. ".repeat(5), ""));

    EnrichmentException exception =
        catchThrowableOfType(
            () -> voaResolver().resolve(voaArticle(serverUrl("/voa-no-byline"))),
            EnrichmentException.class);

    assertThat(exception).hasMessageContaining("required selector is missing");
    assertThat(exception.isRetryable()).isFalse();
  }

  @Test
  void rejectsDisallowedResponseHostAsPermanentFailure() {
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

    assertThat(exception).hasMessageContaining("crawled response URI is not allowed");
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
        sourceProvider,
        new ContentCrawler(Duration.ofSeconds(2), 4096),
        new ContentEligibilityChecker(),
        new ThumbnailEligibilityChecker(),
        20);
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
                thumbnailSelectors)),
        "en",
        SourceType.RSS);
  }

  private CrawlingContentResolver voaResolver() {
    ArticleSourceProvider sourceProvider = mock(ArticleSourceProvider.class);
    when(sourceProvider.findByName("Voice of America")).thenReturn(Optional.of(voaSource()));

    return new CrawlingContentResolver(
        sourceProvider,
        new ContentCrawler(Duration.ofSeconds(2), 4096),
        new ContentEligibilityChecker(),
        new ThumbnailEligibilityChecker(),
        20);
  }

  private ArticleSource voaSource() {
    return ArticleSource.create(
        "Voice of America",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of("https://www.voanews.com/api/"),
                List.of("localhost"),
                List.of("#article-content .wsw"),
                List.of()),
            new EligibilityPolicy(
                List.of(
                    new ArticleEligibilityRule(
                        RuleType.SELECTOR_EXISTS,
                        ".publishing-details .links__item-link",
                        null,
                        List.of()),
                    new ArticleEligibilityRule(
                        RuleType.SELECTOR_ATTRIBUTE_PREFIX_ANY,
                        ".publishing-details .links__item-link",
                        "href",
                        List.of(
                            "/author/",
                            "https://www.voanews.com/author/",
                            "https://voanews.com/author/")),
                    new ArticleEligibilityRule(
                        RuleType.SELECTOR_TEXT_NOT_EQUALS_ANY,
                        ".publishing-details .links__item-link",
                        null,
                        List.of(
                            "VOA News",
                            "AP",
                            "AP News",
                            "AFP",
                            "Reuters",
                            "Associated Press",
                            "Agence France-Presse",
                            "Agence France Presse")),
                    new ArticleEligibilityRule(
                        RuleType.DOCUMENT_HTML_NOT_CONTAINS_ANY,
                        null,
                        null,
                        List.of(
                            "wire service reports",
                            "Associated Press",
                            "AP Photo",
                            "AP News",
                            "Reuters",
                            "Agence France-Presse",
                            "Agence France Presse",
                            "(AFP)"))),
                ThumbnailPolicy.ELIGIBLE_ONLY,
                new ThumbnailEligibilityPolicy(
                    List.of(
                        new ThumbnailCandidateSelector(
                            "figure img", "src", "figure", List.of("figcaption", ".caption"))),
                    List.of("VOA", "Voice of America"),
                    List.of(
                        "Associated Press",
                        "AP Photo",
                        "Reuters",
                        "Agence France-Presse",
                        "AFP",
                        "Getty",
                        "Used with permission")))),
        "en",
        SourceType.RSS);
  }

  private Article article(String contentUrl) {
    return Article.create(contentUrl, "Global Voices", null, "en", PUBLISHED_AT);
  }

  private Article voaArticle(String contentUrl) {
    return Article.create(contentUrl, "Voice of America", null, "en", PUBLISHED_AT);
  }

  private String voaArticleHtml(String bylineHtml, String body, String headHtml) {
    return voaArticleHtml(bylineHtml, body, headHtml, "");
  }

  private String voaArticleHtml(String bylineHtml, String body, String headHtml, String mediaHtml) {
    return """
        <html>
          <head>%s</head>
          <body>
            <div class="publishing-details">
              <ul class="links__list">%s</ul>
            </div>
            <div id="article-content">
              <div class="wsw"><p>%s</p></div>
            </div>
            %s
          </body>
        </html>
        """
        .formatted(headHtml, bylineHtml, body, mediaHtml);
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

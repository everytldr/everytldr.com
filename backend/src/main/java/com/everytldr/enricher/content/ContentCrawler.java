package com.everytldr.enricher.content;

import com.everytldr.enricher.enrichment.EnrichmentException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class ContentCrawler {
  private static final String USER_AGENT = "everytldr-enricher/0.1";
  private static final int MAX_REDIRECTS = 3;

  private final int timeoutMillis;
  private final int maxBodyBytes;

  public ContentCrawler(Duration timeout, int maxBodyBytes) {
    this.timeoutMillis = (int) timeout.toMillis();
    this.maxBodyBytes = maxBodyBytes;
  }

  public CrawledContent crawl(URI initialUri, Predicate<URI> uriValidator) {
    Objects.requireNonNull(initialUri, "initialUri must not be null");
    Objects.requireNonNull(uriValidator, "uriValidator must not be null");

    assertAllowedUri(initialUri, uriValidator);

    Connection session = createSession();
    URI requestUri = initialUri;
    int redirectCount = 0;
    while (true) {
      Connection.Response response = executeRequest(session, requestUri);
      if (isRedirectStatus(response.statusCode())) {
        if (redirectCount == MAX_REDIRECTS) {
          throw EnrichmentException.permanent(
              "article content response exceeded redirect limit: contentUrl=%s, maxRedirects=%d"
                  .formatted(initialUri, MAX_REDIRECTS));
        }

        URI redirectUri = resolveRedirectUri(response.header("Location"), requestUri);
        assertAllowedUri(redirectUri, uriValidator);
        requestUri = redirectUri;
        redirectCount++;
        continue;
      }

      assertResponseStatus(response, initialUri);

      URI responseUri = createResponseUri(response);
      assertAllowedUri(responseUri, uriValidator);

      String contentType = response.contentType() != null ? response.contentType() : "";
      if (!isHtmlContentType(contentType)) {
        throw EnrichmentException.permanent(
            "non-HTML article content response: contentUrl=%s, contentType=%s"
                .formatted(initialUri, contentType));
      }

      assertBodySize(response);
      return new CrawledContent(response.body(), responseUri);
    }
  }

  private Connection createSession() {
    return Jsoup.newSession()
        .userAgent(USER_AGENT)
        .timeout(timeoutMillis)
        .maxBodySize(maxBodyBytes + 1)
        .followRedirects(false)
        .ignoreHttpErrors(true)
        .ignoreContentType(true);
  }

  private Connection.Response executeRequest(Connection session, URI requestUri) {
    try {
      return session.newRequest(requestUri.toString()).execute();
    } catch (SocketTimeoutException e) {
      throw EnrichmentException.retryable("timed out fetching article content: " + requestUri, e);
    } catch (IOException e) {
      throw EnrichmentException.retryable("failed to fetch article content: " + requestUri, e);
    }
  }

  private void assertAllowedUri(URI uri, Predicate<URI> uriValidator) {
    if (!uriValidator.test(uri)) {
      throw EnrichmentException.permanent("crawled request URI is not allowed: " + uri);
    }
  }

  private URI resolveRedirectUri(String location, URI requestUri) {
    if (location == null || location.isBlank()) {
      throw EnrichmentException.permanent(
          "redirect response is missing Location header: contentUrl=" + requestUri);
    }

    try {
      return requestUri.resolve(location);
    } catch (IllegalArgumentException e) {
      throw EnrichmentException.permanent(
          "redirect response Location is invalid: contentUrl=%s, location=%s"
              .formatted(requestUri, location),
          e);
    }
  }

  private boolean isRedirectStatus(int statusCode) {
    return statusCode == 301
        || statusCode == 302
        || statusCode == 303
        || statusCode == 307
        || statusCode == 308;
  }

  private void assertResponseStatus(Connection.Response response, URI initialUri) {
    int statusCode = response.statusCode();
    boolean retryableStatus = statusCode == 408 || statusCode == 429 || statusCode >= 500;
    if (retryableStatus) {
      throw EnrichmentException.retryable(
          "retryable article content response status: %d, contentUrl=%s"
              .formatted(statusCode, initialUri));
    }

    boolean nonSuccessStatus = statusCode < 200 || statusCode >= 300;
    if (nonSuccessStatus) {
      throw EnrichmentException.permanent(
          "non-success article content response status: %d, contentUrl=%s"
              .formatted(statusCode, initialUri));
    }
  }

  private URI createResponseUri(Connection.Response response) {
    try {
      return response.url().toURI();
    } catch (URISyntaxException e) {
      throw EnrichmentException.permanent(
          "invalid response URL after crawling: " + response.url(), e);
    }
  }

  private void assertBodySize(Connection.Response response) {
    String contentLengthStr = response.header("Content-Length");
    if (contentLengthStr != null) {
      try {
        if (Long.parseLong(contentLengthStr) > maxBodyBytes) {
          throw EnrichmentException.permanent(
              "article content response is too large: responseUrl=%s".formatted(response.url()));
        }
      } catch (NumberFormatException ignored) {
      }
    }

    if (response.bodyAsBytes().length > maxBodyBytes) {
      throw EnrichmentException.permanent(
          "article content response is too large: responseUrl=%s".formatted(response.url()));
    }
  }

  private boolean isHtmlContentType(String contentType) {
    String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    return "text/html".equals(mediaType) || "application/xhtml+xml".equals(mediaType);
  }

  public record CrawledContent(String html, URI finalUri) {}
}

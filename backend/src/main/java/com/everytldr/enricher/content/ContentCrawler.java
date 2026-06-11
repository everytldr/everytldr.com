package com.everytldr.enricher.content;

import com.everytldr.enricher.enrichment.EnrichmentException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

@Slf4j
public class ContentCrawler {
  private static final String USER_AGENT = "everytldr-enricher/0.1";

  private final int timeoutMillis;
  private final int maxBodyBytes;

  public ContentCrawler(Duration timeout, int maxBodyBytes) {
    this.timeoutMillis = (int) timeout.toMillis();
    this.maxBodyBytes = maxBodyBytes;
  }

  public String crawl(URI uri, Predicate<URI> uriValidator) {
    Connection.Response response;
    try {
      response =
          Jsoup.connect(uri.toString())
              .userAgent(USER_AGENT)
              .timeout(timeoutMillis)
              .maxBodySize(maxBodyBytes + 1)
              .followRedirects(true)
              .ignoreHttpErrors(true)
              .ignoreContentType(true)
              .execute();
    } catch (SocketTimeoutException e) {
      throw EnrichmentException.retryable("timed out fetching article content: " + uri, e);
    } catch (IOException e) {
      throw EnrichmentException.retryable("failed to fetch article content: " + uri, e);
    }

    int statusCode = response.statusCode();
    boolean retryableStatus = statusCode == 408 || statusCode == 429 || statusCode >= 500;
    if (retryableStatus) {
      throw EnrichmentException.retryable(
          "retryable article content response status: %d, contentUrl=%s"
              .formatted(statusCode, uri));
    }

    boolean nonSuccessStatus = statusCode < 200 || statusCode >= 300;
    if (nonSuccessStatus) {
      throw EnrichmentException.permanent(
          "non-success article content response status: %d, contentUrl=%s"
              .formatted(statusCode, uri));
    }

    URI responseUri = createResponseUri(response);
    if (!uriValidator.test(responseUri)) {
      throw EnrichmentException.permanent("crawled response URI is not allowed: " + responseUri);
    }

    String contentType = response.contentType() != null ? response.contentType() : "";
    if (!isHtmlContentType(contentType)) {
      throw EnrichmentException.permanent(
          "non-HTML article content response: contentUrl=%s, contentType=%s"
              .formatted(uri, contentType));
    }

    assertBodySize(response);

    return response.body();
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
}

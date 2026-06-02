package com.everytldr.enricher.enrichment;

import com.everytldr.common.domain.article.Article;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fetches allowlisted HTTP(S) article pages and converts extracted HTML text into ArticleContent.
 */
@Component
@Profile("enricher")
@Slf4j
public class ArticlePageContentResolver implements ArticleContentResolver {
  private static final String USER_AGENT = "everytldr-enricher/0.1";

  private final EnricherContentProperties properties;
  private final HttpClient httpClient;

  public ArticlePageContentResolver(EnricherContentProperties properties) {
    this.properties = properties;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(properties.requestTimeout())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  @Override
  public boolean supports(Article article) {
    if (article == null || !StringUtils.hasText(article.getSourceUrl())) {
      return false;
    }

    Optional<URI> uri = parseUri(article.getSourceUrl());
    return uri.filter(this::isSupportedUri).isPresent();
  }

  @Override
  public ArticleContent resolve(Article article) {
    URI sourceUri =
        parseUri(article.getSourceUrl())
            .filter(this::isSupportedUri)
            .orElseThrow(
                () ->
                    ArticleEnrichmentException.permanent(
                        "unsupported source URL for enrichment: " + article.getSourceUrl()));

    String html = fetchHtml(sourceUri);
    String body = extractBody(html, sourceUri);
    if (body.length() < properties.minBodyChars()) {
      throw ArticleEnrichmentException.permanent(
          "extracted article body is too short: sourceUrl=%s, length=%d"
              .formatted(article.getSourceUrl(), body.length()));
    }

    return new ArticleContent(
        article.getSourceUrl(), article.getSource(), article.getLanguage(), body);
  }

  private String fetchHtml(URI sourceUri) {
    URI currentUri = sourceUri;
    int redirects = 0;

    while (true) {
      HttpResponse<InputStream> response = send(currentUri);
      int statusCode = response.statusCode();

      if (isRedirect(statusCode)) {
        if (redirects >= properties.maxRedirects()) {
          closeQuietly(response.body());
          throw ArticleEnrichmentException.permanent(
              "redirect limit exceeded while fetching article content: " + sourceUri);
        }
        currentUri = resolveRedirect(currentUri, response);
        redirects++;
        continue;
      }

      if (isRetryableStatus(statusCode)) {
        closeQuietly(response.body());
        throw ArticleEnrichmentException.retryable(
            "retryable article content response status: %d, sourceUrl=%s"
                .formatted(statusCode, currentUri));
      }
      if (statusCode < 200 || statusCode >= 300) {
        closeQuietly(response.body());
        throw ArticleEnrichmentException.permanent(
            "non-success article content response status: %d, sourceUrl=%s"
                .formatted(statusCode, currentUri));
      }

      String contentType = resolveContentType(response);
      if (!isHtmlContent(contentType)) {
        closeQuietly(response.body());
        throw ArticleEnrichmentException.permanent(
            "non-HTML article content response: sourceUrl=%s, contentType=%s"
                .formatted(currentUri, contentType));
      }

      return readBody(response, currentUri, contentType);
    }
  }

  private HttpResponse<InputStream> send(URI uri) {
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(properties.requestTimeout())
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();

    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (HttpTimeoutException e) {
      throw ArticleEnrichmentException.retryable("timed out fetching article content: " + uri, e);
    } catch (IOException e) {
      throw ArticleEnrichmentException.retryable("failed to fetch article content: " + uri, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw ArticleEnrichmentException.retryable("interrupted fetching article content: " + uri, e);
    }
  }

  private URI resolveRedirect(URI currentUri, HttpResponse<InputStream> response) {
    closeQuietly(response.body());
    String location =
        response
            .headers()
            .firstValue("location")
            .orElseThrow(
                () ->
                    ArticleEnrichmentException.permanent(
                        "redirect response is missing Location header: " + currentUri));

    URI redirectUri;
    try {
      redirectUri = currentUri.resolve(location);
    } catch (IllegalArgumentException e) {
      throw ArticleEnrichmentException.permanent(
          "redirect Location is invalid: sourceUrl=%s, location=%s".formatted(currentUri, location),
          e);
    }

    if (!isSupportedUri(redirectUri)) {
      throw ArticleEnrichmentException.permanent(
          "redirect target is not allowed for article content: sourceUrl=%s, location=%s"
              .formatted(currentUri, redirectUri));
    }
    return redirectUri;
  }

  private String readBody(HttpResponse<InputStream> response, URI sourceUri, String contentType) {
    long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1L);
    if (contentLength > properties.maxBodyBytes()) {
      closeQuietly(response.body());
      throw ArticleEnrichmentException.permanent(
          "article content response is too large: sourceUrl=%s, contentLength=%d"
              .formatted(sourceUri, contentLength));
    }

    try (InputStream body = response.body()) {
      byte[] bytes = body.readNBytes(properties.maxBodyBytes() + 1);
      if (bytes.length > properties.maxBodyBytes()) {
        throw ArticleEnrichmentException.permanent(
            "article content response is too large: sourceUrl=%s".formatted(sourceUri));
      }
      return new String(bytes, resolveCharset(contentType));
    } catch (IOException e) {
      throw ArticleEnrichmentException.retryable(
          "failed to read article content response: " + sourceUri, e);
    }
  }

  private String extractBody(String html, URI sourceUri) {
    Document document = Jsoup.parse(html, sourceUri.toString());
    document.select("script, style, noscript").remove();

    Element bodyElement = findFirstNonEmpty(document.selectFirst("article"));
    if (bodyElement == null) {
      bodyElement = findFirstNonEmpty(document.selectFirst("main"));
    }
    if (bodyElement == null) {
      bodyElement = findFirstNonEmpty(document.body());
    }
    if (bodyElement == null) {
      return "";
    }
    return bodyElement.text().replaceAll("\\s+", " ").trim();
  }

  private Element findFirstNonEmpty(Element element) {
    if (element == null || !StringUtils.hasText(element.text())) {
      return null;
    }
    return element;
  }

  private boolean isSupportedUri(URI uri) {
    if (uri == null || uri.getHost() == null) {
      return false;
    }

    String scheme = uri.getScheme();
    if (scheme == null) {
      return false;
    }

    String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
    return ("http".equals(normalizedScheme) || "https".equals(normalizedScheme))
        && properties.isAllowedHost(uri.getHost());
  }

  private Optional<URI> parseUri(String sourceUrl) {
    try {
      return Optional.of(URI.create(sourceUrl));
    } catch (IllegalArgumentException e) {
      log.debug("Article source URL is malformed. sourceUrl={}", sourceUrl);
      return Optional.empty();
    }
  }

  private boolean isRedirect(int statusCode) {
    return statusCode >= 300 && statusCode < 400;
  }

  private boolean isRetryableStatus(int statusCode) {
    return statusCode == 408 || statusCode == 429 || statusCode >= 500;
  }

  private String resolveContentType(HttpResponse<?> response) {
    return response.headers().firstValue("content-type").orElse("");
  }

  private boolean isHtmlContent(String contentType) {
    String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    return "text/html".equals(mediaType) || "application/xhtml+xml".equals(mediaType);
  }

  private Charset resolveCharset(String contentType) {
    for (String part : contentType.split(";")) {
      String trimmed = part.trim();
      int separator = trimmed.indexOf('=');
      if (separator < 0) {
        continue;
      }

      String key = trimmed.substring(0, separator).trim().toLowerCase(Locale.ROOT);
      if (!"charset".equals(key)) {
        continue;
      }

      String value = trimmed.substring(separator + 1).trim().replace("\"", "");
      try {
        return Charset.forName(value);
      } catch (IllegalArgumentException e) {
        return StandardCharsets.UTF_8;
      }
    }
    return StandardCharsets.UTF_8;
  }

  private void closeQuietly(InputStream body) {
    if (body == null) {
      return;
    }
    try {
      body.close();
    } catch (IOException e) {
      log.debug("Failed to close article content response body", e);
    }
  }
}

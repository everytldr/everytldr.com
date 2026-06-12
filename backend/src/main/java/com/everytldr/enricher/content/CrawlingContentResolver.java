package com.everytldr.enricher.content;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.enricher.enrichment.EnrichmentException;
import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.StringUtils;

@Slf4j
public class CrawlingContentResolver implements ContentResolver {
  private final ArticleSourceProvider articleSourceProvider;
  private final ContentCrawler contentCrawler;
  private final int minBodyChars;

  public CrawlingContentResolver(
      ArticleSourceProvider articleSourceProvider,
      ContentCrawler contentCrawler,
      int minBodyChars) {
    this.articleSourceProvider = articleSourceProvider;
    this.contentCrawler = contentCrawler;
    this.minBodyChars = minBodyChars;
  }

  @Override
  public boolean supports(Article article) {
    if (article == null || !StringUtils.hasText(article.getContentUrl())) {
      return false;
    }

    Optional<URI> contentUri = parseUri(article.getContentUrl());
    if (contentUri.isEmpty()) {
      return false;
    }

    return articleSourceProvider
        .findByName(article.getSource())
        .map(source -> source.getPolicy().crawling().isAllowedHost(contentUri.get().getHost()))
        .orElse(false);
  }

  @Override
  public String resolve(Article article) {
    URI contentUri = URI.create(article.getContentUrl());

    ArticleSource source = articleSourceProvider.findByName(article.getSource()).orElseThrow();
    if (!source.isActive()) {
      throw EnrichmentException.permanent("article source is inactive: " + article.getSource());
    }

    CrawlingPolicy policy = source.getPolicy().crawling();

    String html =
        contentCrawler.crawl(
            contentUri, responseUri -> policy.isAllowedHost(responseUri.getHost()));

    String content = extractContent(html, contentUri, source);
    assertMinBodyChars(content, article.getContentUrl());

    return content;
  }

  private void assertMinBodyChars(String content, String contentUrl) {
    int contentLength = content.length();
    if (contentLength < minBodyChars) {
      throw EnrichmentException.permanent(
          "extracted article body is too short: contentUrl=%s, length=%d"
              .formatted(contentUrl, contentLength));
    }
  }

  private String extractContent(String html, URI contentUri, ArticleSource source) {
    Document document = Jsoup.parse(html, contentUri.toString());
    document.select("script, style, noscript").remove();

    for (String selector : source.getPolicy().crawling().selectors()) {
      Optional<String> content =
          document.select(selector).stream()
              .map(element -> element.text().replaceAll("\\s+", " ").trim())
              .filter(StringUtils::hasText)
              .max((left, right) -> Integer.compare(left.length(), right.length()));
      if (content.isPresent()) {
        return content.get();
      }
    }
    throw EnrichmentException.permanent(
        "failed to extract article body: contentUrl=%s".formatted(contentUri));
  }

  private Optional<URI> parseUri(String contentUrl) {
    try {
      return Optional.of(URI.create(contentUrl));
    } catch (IllegalArgumentException e) {
      log.debug("Article content URL is malformed. contentUrl={}", contentUrl);
      return Optional.empty();
    }
  }
}

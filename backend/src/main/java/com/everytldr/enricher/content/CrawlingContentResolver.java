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
import org.jsoup.nodes.Element;
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
        .map(source -> source.getPolicy().crawling().isAllowedContentUri(contentUri.get()))
        .orElse(false);
  }

  @Override
  public ResolvedArticle resolve(Article article) {
    URI contentUri = URI.create(article.getContentUrl());

    ArticleSource source = articleSourceProvider.findByName(article.getSource()).orElseThrow();
    if (!source.isActive()) {
      throw EnrichmentException.permanent("article source is inactive: " + article.getSource());
    }

    CrawlingPolicy policy = source.getPolicy().crawling();

    String html = contentCrawler.crawl(contentUri, policy::isAllowedContentUri);

    Document document = Jsoup.parse(html, contentUri.toString());
    document.select("script, style, noscript").remove();

    Optional<String> extracted = extractContent(document, source);
    if (extracted.isEmpty()) {
      throw EnrichmentException.permanent(
          "failed to extract article body: contentUrl=%s".formatted(contentUri));
    }
    String content = extracted.get();
    assertMinBodyChars(content, article.getContentUrl());

    boolean hasThumbnailUrl = StringUtils.hasText(article.getThumbnailUrl());
    if (hasThumbnailUrl) {
      return new ResolvedArticle(content, null);
    }

    String thumbnailUrl = extractThumbnailUrl(document, policy).orElse(null);
    return new ResolvedArticle(content, thumbnailUrl);
  }

  private void assertMinBodyChars(String content, String contentUrl) {
    int contentLength = content.length();
    if (contentLength < minBodyChars) {
      throw EnrichmentException.permanent(
          "extracted article body is too short: contentUrl=%s, length=%d"
              .formatted(contentUrl, contentLength));
    }
  }

  private Optional<String> extractThumbnailUrl(Document document, CrawlingPolicy policy) {
    for (String selector : policy.thumbnailSelectors()) {
      Element image = document.selectFirst(selector);
      if (image != null) {
        String imageUrl = image.absUrl("src");
        if (StringUtils.hasText(imageUrl)) {
          return Optional.of(imageUrl);
        }
      }
    }

    Element ogImage = document.selectFirst("meta[property=\"og:image\"]");
    if (ogImage != null) {
      String ogImageUrl = ogImage.absUrl("content");
      if (StringUtils.hasText(ogImageUrl)) {
        return Optional.of(ogImageUrl);
      }
    }

    return Optional.empty();
  }

  private Optional<String> extractContent(Document document, ArticleSource source) {
    for (String selector : source.getPolicy().crawling().contentSelectors()) {
      Optional<String> content =
          document.select(selector).stream()
              .map(element -> element.text().replaceAll("\\s+", " ").trim())
              .filter(StringUtils::hasText)
              .max((left, right) -> Integer.compare(left.length(), right.length()));
      if (content.isPresent()) {
        return content;
      }
    }
    return Optional.empty();
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

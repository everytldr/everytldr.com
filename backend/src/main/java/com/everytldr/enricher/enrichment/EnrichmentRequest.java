package com.everytldr.enricher.enrichment;

import com.everytldr.common.domain.article.Article;
import java.util.List;
import java.util.Objects;

public record EnrichmentRequest(
    String contentUrl, String source, String language, String content, List<String> categorySlugs) {
  public EnrichmentRequest {
    requireText(contentUrl, "contentUrl");
    requireText(source, "source");
    requireText(language, "language");
    requireText(content, "content");
    if (categorySlugs == null || categorySlugs.isEmpty()) {
      throw new IllegalArgumentException("categorySlugs must not be empty");
    }
    categorySlugs.forEach(categorySlug -> requireText(categorySlug, "categorySlug"));
    categorySlugs = List.copyOf(categorySlugs);
  }

  public static EnrichmentRequest from(
      Article article, String content, List<String> categorySlugs) {
    return new EnrichmentRequest(
        article.getContentUrl(),
        article.getSource(),
        article.getLanguage(),
        content,
        categorySlugs);
  }

  private static void requireText(String value, String fieldName) {
    Objects.requireNonNull(value, "%s must not be null".formatted(fieldName));
    if (value.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
  }
}

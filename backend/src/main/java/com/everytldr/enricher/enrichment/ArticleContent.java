package com.everytldr.enricher.enrichment;

import java.util.Objects;

public record ArticleContent(String sourceUrl, String source, String language, String body) {
  public ArticleContent {
    requireText(sourceUrl, "sourceUrl");
    requireText(source, "source");
    requireText(language, "language");
    requireText(body, "body");
  }

  private static void requireText(String value, String fieldName) {
    Objects.requireNonNull(value, "%s must not be null".formatted(fieldName));
    if (value.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
  }
}

package com.everytldr.enricher.enrichment;

import java.util.List;
import java.util.Objects;

public record ArticleEnrichmentRequest(
    ArticleContent content, List<ArticleEnrichmentCategoryOption> categories) {
  public ArticleEnrichmentRequest {
    Objects.requireNonNull(content, "content must not be null");
    if (categories == null || categories.isEmpty()) {
      throw new IllegalArgumentException("categories must not be empty");
    }
    categories = List.copyOf(categories);
  }
}

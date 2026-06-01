package org.everytldr.enricher.enrichment;

import java.util.Objects;

public record ArticleEnrichmentCategoryOption(String slug) {
  public ArticleEnrichmentCategoryOption {
    Objects.requireNonNull(slug, "slug must not be null");
    if (slug.isBlank()) {
      throw new IllegalArgumentException("slug must not be blank");
    }
  }
}

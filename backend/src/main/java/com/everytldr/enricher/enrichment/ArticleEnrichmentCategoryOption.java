package com.everytldr.enricher.enrichment;

import com.everytldr.common.domain.category.Category;
import java.util.Objects;

public record ArticleEnrichmentCategoryOption(String slug) {
  public ArticleEnrichmentCategoryOption {
    Objects.requireNonNull(slug, "slug must not be null");
    if (slug.isBlank()) {
      throw new IllegalArgumentException("slug must not be blank");
    }
  }

  public static ArticleEnrichmentCategoryOption from(Category category) {
    Objects.requireNonNull(category, "category must not be null");
    return new ArticleEnrichmentCategoryOption(category.getSlug());
  }
}

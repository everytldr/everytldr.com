package org.everytldr.enricher.enrichment;

import java.util.Objects;
import org.everytldr.common.domain.category.Category;

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

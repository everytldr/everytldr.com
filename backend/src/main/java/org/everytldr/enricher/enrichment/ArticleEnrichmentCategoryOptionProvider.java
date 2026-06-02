package org.everytldr.enricher.enrichment;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.everytldr.common.domain.category.CategoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class ArticleEnrichmentCategoryOptionProvider {
  public static final String CACHE_NAME = "enricherCategoryOptions";

  private final CategoryRepository categoryRepository;

  @Cacheable(cacheNames = CACHE_NAME, sync = true)
  public List<ArticleEnrichmentCategoryOption> getCategoryOptions() {
    List<ArticleEnrichmentCategoryOption> categoryOptions =
        categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
            .map(ArticleEnrichmentCategoryOption::from)
            .toList();

    if (categoryOptions.isEmpty()) {
      throw ArticleEnrichmentException.permanent("no categories configured for enrichment");
    }
    return categoryOptions;
  }
}

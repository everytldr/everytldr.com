package com.everytldr.enricher.enrichment;

import com.everytldr.common.domain.category.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class CategorySlugProvider {
  public static final String CACHE_NAME = "enricherCategorySlugs";

  private final CategoryRepository categoryRepository;

  @Cacheable(cacheNames = CACHE_NAME, sync = true)
  public List<String> getCategorySlugs() {
    List<String> categorySlugs =
        categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
            .map(category -> category.getSlug())
            .toList();

    if (categorySlugs.isEmpty()) {
      throw EnrichmentException.permanent("no categories configured for enrichment");
    }
    return categorySlugs;
  }
}

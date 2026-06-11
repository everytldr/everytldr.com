package com.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorySlugProviderTest {

  private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
  private final CategorySlugProvider provider = new CategorySlugProvider(categoryRepository);

  @Test
  void returnsCategorySlugsInRepositoryOrder() {
    when(categoryRepository.findAllByOrderBySortOrderAscIdAsc())
        .thenReturn(List.of(Category.create("global-voices", 0), Category.create("sports", 10)));

    assertThat(provider.getCategorySlugs()).containsExactly("global-voices", "sports");
  }

  @Test
  void failsWhenNoCategoriesAreConfigured() {
    when(categoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

    assertThatThrownBy(provider::getCategorySlugs)
        .isInstanceOf(EnrichmentException.class)
        .hasMessage("no categories configured for enrichment")
        .satisfies(
            exception -> assertThat(((EnrichmentException) exception).isRetryable()).isFalse());
  }
}

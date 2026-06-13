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
    when(categoryRepository.findAllByOrderBySlugAsc())
        .thenReturn(List.of(Category.create("media"), Category.create("sport")));

    assertThat(provider.getCategorySlugs()).containsExactly("media", "sport");
  }

  @Test
  void failsWhenNoCategoriesAreConfigured() {
    when(categoryRepository.findAllByOrderBySlugAsc()).thenReturn(List.of());

    assertThatThrownBy(provider::getCategorySlugs)
        .isInstanceOf(EnrichmentException.class)
        .hasMessage("no categories configured for enrichment")
        .satisfies(
            exception -> assertThat(((EnrichmentException) exception).isRetryable()).isFalse());
  }
}

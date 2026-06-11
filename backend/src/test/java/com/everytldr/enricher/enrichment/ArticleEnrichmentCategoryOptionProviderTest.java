package com.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
import com.everytldr.enricher.EnricherConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ArticleEnrichmentCategoryOptionProviderTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withInitializer(context -> context.getEnvironment().setActiveProfiles("enricher"))
          .withUserConfiguration(
              EnricherConfig.class,
              ArticleEnrichmentCategoryOptionProvider.class,
              CategoryRepositoryTestConfig.class)
          .withPropertyValues(commonProperties());

  @Test
  void loadsCategoryOptionsInRepositoryOrder() {
    CategoryRepository categoryRepository = mock(CategoryRepository.class);
    ArticleEnrichmentCategoryOptionProvider provider =
        new ArticleEnrichmentCategoryOptionProvider(categoryRepository);

    when(categoryRepository.findAllByOrderBySortOrderAscIdAsc())
        .thenReturn(List.of(Category.create("citizen_media", 0), Category.create("politics", 10)));

    List<ArticleEnrichmentCategoryOption> categoryOptions = provider.getCategoryOptions();

    assertThat(categoryOptions)
        .containsExactly(
            new ArticleEnrichmentCategoryOption("citizen_media"),
            new ArticleEnrichmentCategoryOption("politics"));
  }

  @Test
  void failsPermanentlyWhenNoCategoriesAreConfigured() {
    CategoryRepository categoryRepository = mock(CategoryRepository.class);
    ArticleEnrichmentCategoryOptionProvider provider =
        new ArticleEnrichmentCategoryOptionProvider(categoryRepository);

    when(categoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

    assertThatThrownBy(provider::getCategoryOptions)
        .isInstanceOf(ArticleEnrichmentException.class)
        .hasMessage("no categories configured for enrichment")
        .satisfies(
            exception ->
                assertThat(((ArticleEnrichmentException) exception).isRetryable()).isFalse());
  }

  @Test
  void cachesCategoryOptionsInsideConfiguredTtl() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(CacheManager.class);
          assertThat(context).hasSingleBean(ArticleEnrichmentCategoryOptionProvider.class);

          CategoryRepository categoryRepository = context.getBean(CategoryRepository.class);
          when(categoryRepository.findAllByOrderBySortOrderAscIdAsc())
              .thenReturn(List.of(Category.create("citizen_media", 0)));

          ArticleEnrichmentCategoryOptionProvider provider =
              context.getBean(ArticleEnrichmentCategoryOptionProvider.class);

          List<ArticleEnrichmentCategoryOption> firstCall = provider.getCategoryOptions();
          List<ArticleEnrichmentCategoryOption> secondCall = provider.getCategoryOptions();

          assertThat(firstCall)
              .containsExactly(new ArticleEnrichmentCategoryOption("citizen_media"));
          assertThat(secondCall).isEqualTo(firstCall);
          verify(categoryRepository, times(1)).findAllByOrderBySortOrderAscIdAsc();
        });
  }

  private String[] commonProperties() {
    return new String[] {
      "everytldr.enricher.processing.enabled=false",
      "everytldr.enricher.processing.batch-size=10",
      "everytldr.enricher.processing.fixed-delay=30s",
      "everytldr.enricher.processing.max-attempts=3",
      "everytldr.enricher.processing.retry-delay=10m",
      "everytldr.enricher.processing.stale-timeout=15m",
      "everytldr.enricher.content.allowed-hosts=localhost,globalvoices.org",
      "everytldr.enricher.content.request-timeout=5s",
      "everytldr.enricher.content.max-redirects=3",
      "everytldr.enricher.content.max-body-bytes=1048576",
      "everytldr.enricher.content.min-body-chars=200",
      "everytldr.enricher.cache.category-options.ttl=5m"
    };
  }

  @Configuration
  static class CategoryRepositoryTestConfig {
    @Bean
    CategoryRepository categoryRepository() {
      return mock(CategoryRepository.class);
    }
  }
}

package com.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.common.domain.category.Category;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleEnrichmentRequestTest {

  @Test
  void requestRequiresAtLeastOneCategory() {
    ArticleContent content = content();

    assertThatThrownBy(() -> new ArticleEnrichmentRequest(content, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categories must not be empty");
  }

  @Test
  void requestDefensivelyCopiesCategoryOptions() {
    ArticleContent content = content();
    List<ArticleEnrichmentCategoryOption> categories = new ArrayList<>();
    categories.add(new ArticleEnrichmentCategoryOption("global-voices"));

    ArticleEnrichmentRequest request = new ArticleEnrichmentRequest(content, categories);
    categories.add(new ArticleEnrichmentCategoryOption("global-voices-politics"));

    assertThat(request.categories())
        .containsExactly(new ArticleEnrichmentCategoryOption("global-voices"));
  }

  @Test
  void categoryOptionRequiresNonBlankSlug() {
    assertThatThrownBy(() -> new ArticleEnrichmentCategoryOption(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("slug must not be blank");
  }

  @Test
  void categoryOptionCanBeCreatedFromCategory() {
    Category category = Category.create("global-voices-politics", 10);

    ArticleEnrichmentCategoryOption option = ArticleEnrichmentCategoryOption.from(category);

    assertThat(option).isEqualTo(new ArticleEnrichmentCategoryOption("global-voices-politics"));
  }

  private ArticleContent content() {
    return new ArticleContent(
        "https://globalvoices.org/example", "Global Voices", "en", "Full article body");
  }
}

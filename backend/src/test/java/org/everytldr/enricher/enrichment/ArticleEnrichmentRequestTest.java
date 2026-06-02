package org.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.everytldr.common.domain.category.Category;
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
    categories.add(new ArticleEnrichmentCategoryOption("sport-football"));

    ArticleEnrichmentRequest request = new ArticleEnrichmentRequest(content, categories);
    categories.add(new ArticleEnrichmentCategoryOption("sport-football-epl"));

    assertThat(request.categories())
        .containsExactly(new ArticleEnrichmentCategoryOption("sport-football"));
  }

  @Test
  void categoryOptionRequiresNonBlankSlug() {
    assertThatThrownBy(() -> new ArticleEnrichmentCategoryOption(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("slug must not be blank");
  }

  @Test
  void categoryOptionCanBeCreatedFromCategory() {
    Category category = Category.create("sport-football-epl", 10);

    ArticleEnrichmentCategoryOption option = ArticleEnrichmentCategoryOption.from(category);

    assertThat(option).isEqualTo(new ArticleEnrichmentCategoryOption("sport-football-epl"));
  }

  private ArticleContent content() {
    return new ArticleContent(
        "https://www.theguardian.com/football/example",
        "The Guardian Football",
        "en",
        "Full article body");
  }
}

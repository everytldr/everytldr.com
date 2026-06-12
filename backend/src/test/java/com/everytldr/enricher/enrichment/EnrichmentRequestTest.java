package com.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnrichmentRequestTest {

  @Test
  void requestRequiresAtLeastOneCategory() {
    assertThatThrownBy(() -> request(List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categorySlugs must not be empty");
  }

  @Test
  void requestRequiresNonBlankContent() {
    assertThatThrownBy(
            () -> {
              List<String> categorySlugs = List.of("global-voices");
              new EnrichmentRequest(
                  "https://globalvoices.org/example", "Global Voices", "en", " ", categorySlugs);
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("content must not be blank");
  }

  @Test
  void requestDefensivelyCopiesCategorySlugs() {
    List<String> categorySlugs = new ArrayList<>();
    categorySlugs.add("global-voices");

    EnrichmentRequest request = request(categorySlugs);
    categorySlugs.add("global-voices-politics");

    assertThat(request.categorySlugs()).containsExactly("global-voices");
  }

  @Test
  void requestRequiresNonBlankCategorySlug() {
    assertThatThrownBy(() -> request(List.of(" ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categorySlug must not be blank");
  }

  private EnrichmentRequest request(List<String> categorySlugs) {
    return new EnrichmentRequest(
        "https://globalvoices.org/example",
        "Global Voices",
        "en",
        "Full article body",
        categorySlugs);
  }
}

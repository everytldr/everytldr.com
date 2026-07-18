package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArticleViewPropertiesTest {

  @Test
  void acceptsPositiveDeduplicationTtl() {
    new ArticleViewProperties(Duration.ofHours(24));
  }

  @Test
  void rejectsNonPositiveDeduplicationTtl() {
    assertThatThrownBy(() -> new ArticleViewProperties(Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("deduplicationTtl must be positive");
  }
}

package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArticleViewMemoryGuardPropertiesTest {

  @Test
  void acceptsUsableSettings() {
    assertThatCode(() -> new ArticleViewMemoryGuardProperties(0.9, Duration.ofSeconds(10)))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsThresholdOutsideZeroToOneRange() {
    assertThatThrownBy(() -> new ArticleViewMemoryGuardProperties(0, Duration.ofSeconds(10)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("threshold must be greater than 0 and at most 1");
  }

  @Test
  void rejectsNonPositiveSampleInterval() {
    assertThatThrownBy(() -> new ArticleViewMemoryGuardProperties(0.9, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sampleInterval must be positive");
  }
}

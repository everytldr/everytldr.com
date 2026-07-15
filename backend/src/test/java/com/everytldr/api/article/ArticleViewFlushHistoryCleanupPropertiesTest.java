package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArticleViewFlushHistoryCleanupPropertiesTest {

  @Test
  void acceptsUsableSettings() {
    assertThatCode(
            () ->
                new ArticleViewFlushHistoryCleanupProperties(
                    Duration.ofDays(30), Duration.ofDays(1), 1_000))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsNonPositiveRetention() {
    assertThatThrownBy(
            () ->
                new ArticleViewFlushHistoryCleanupProperties(
                    Duration.ZERO, Duration.ofDays(1), 1_000))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("retention must be positive");
  }

  @Test
  void rejectsNonPositiveBatchSize() {
    assertThatThrownBy(
            () ->
                new ArticleViewFlushHistoryCleanupProperties(
                    Duration.ofDays(30), Duration.ofDays(1), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("batchSize must be positive");
  }
}

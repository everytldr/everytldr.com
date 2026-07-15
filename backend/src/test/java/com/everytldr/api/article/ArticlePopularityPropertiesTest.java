package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArticlePopularityPropertiesTest {

  @Test
  void acceptsPositiveBucketTtlAndLookback() {
    assertThatCode(() -> new ArticlePopularityProperties(Duration.ofHours(26), 24))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsNonPositiveBucketTtl() {
    assertThatThrownBy(() -> new ArticlePopularityProperties(Duration.ZERO, 24))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bucketTtl must be positive");
  }

  @Test
  void rejectsBucketTtlWithoutMillisecondPrecision() {
    assertThatThrownBy(() -> new ArticlePopularityProperties(Duration.ofNanos(1), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bucketTtl must be at least 1 millisecond");
  }

  @Test
  void rejectsBucketTtlShorterThanLookbackWindow() {
    assertThatThrownBy(() -> new ArticlePopularityProperties(Duration.ofHours(24), 24))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bucketTtl must be at least PT25H for the configured lookback");
  }

  @Test
  void rejectsNegativeLookbackHours() {
    assertThatThrownBy(() -> new ArticlePopularityProperties(Duration.ofHours(26), -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bucketLookbackHours must not be negative");
  }

  @Test
  void rejectsLookbackHoursOverSevenDays() {
    assertThatThrownBy(() -> new ArticlePopularityProperties(Duration.ofHours(170), 169))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bucketLookbackHours must not exceed 168");
  }

  @Test
  void rejectsNullBucketTtl() {
    assertThatThrownBy(() -> new ArticlePopularityProperties(null, 24))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("bucketTtl must not be null");
  }
}

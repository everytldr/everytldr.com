package com.everytldr.scheduler.article.view;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArticleViewFlushPropertiesTest {

  @Test
  void acceptsHourlyFlushCron() {
    assertThatCode(() -> new ArticleViewFlushProperties(true, "0 0 * * * *"))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsBlankCron() {
    assertThatThrownBy(() -> new ArticleViewFlushProperties(true, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cron must not be blank");
  }

  @Test
  void rejectsInvalidCron() {
    assertThatThrownBy(() -> new ArticleViewFlushProperties(true, "not-a-cron"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cron must be a valid cron expression");
  }
}

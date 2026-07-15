package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArticleViewPropertiesTest {

  @Test
  void acceptsHourlyFlushCron() {
    assertThatCode(() -> createProperties("0 0 * * * *")).doesNotThrowAnyException();
  }

  @Test
  void rejectsBlankFlushCron() {
    assertThatThrownBy(() -> createProperties(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("flushCron must not be blank");
  }

  @Test
  void rejectsInvalidFlushCron() {
    assertThatThrownBy(() -> createProperties("not-a-cron"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("flushCron must be a valid cron expression");
  }

  private static ArticleViewProperties createProperties(String flushCron) {
    return new ArticleViewProperties(Duration.ofHours(24), flushCron);
  }
}

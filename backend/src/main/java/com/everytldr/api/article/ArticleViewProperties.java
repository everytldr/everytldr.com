package com.everytldr.api.article;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;

@ConfigurationProperties(prefix = "everytldr.article-view")
public record ArticleViewProperties(Duration deduplicationTtl, String flushCron) {
  public ArticleViewProperties {
    assertPositive(deduplicationTtl, "deduplicationTtl");
    assertValidCron(flushCron);
  }

  private static void assertPositive(Duration value, String name) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private static void assertValidCron(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("flushCron must not be blank");
    }
    try {
      CronExpression.parse(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("flushCron must be a valid cron expression", e);
    }
  }
}

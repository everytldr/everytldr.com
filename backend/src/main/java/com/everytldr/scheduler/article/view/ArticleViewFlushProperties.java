package com.everytldr.scheduler.article.view;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;

@ConfigurationProperties(prefix = "everytldr.article-view.flush")
public record ArticleViewFlushProperties(boolean enabled, String cron) {
  public ArticleViewFlushProperties {
    if (cron == null || cron.isBlank()) {
      throw new IllegalArgumentException("cron must not be blank");
    }
    try {
      CronExpression.parse(cron);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("cron must be a valid cron expression", e);
    }
  }
}

package com.everytldr.scheduler.briefing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;

@ConfigurationProperties(prefix = "everytldr.briefing.generation")
public record BriefingGenerationProperties(boolean enabled, String cron, int articleCount) {
  public BriefingGenerationProperties {
    if (cron == null || cron.isBlank()) {
      throw new IllegalArgumentException("cron must not be blank");
    }
    try {
      CronExpression.parse(cron);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("cron must be a valid cron expression", e);
    }
    if (articleCount <= 0) {
      throw new IllegalArgumentException("articleCount must be positive");
    }
  }
}

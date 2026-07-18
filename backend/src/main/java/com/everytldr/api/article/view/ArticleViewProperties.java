package com.everytldr.api.article.view;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.article-view")
public record ArticleViewProperties(Duration deduplicationTtl) {
  public ArticleViewProperties {
    assertPositive(deduplicationTtl, "deduplicationTtl");
  }

  private static void assertPositive(Duration value, String name) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}

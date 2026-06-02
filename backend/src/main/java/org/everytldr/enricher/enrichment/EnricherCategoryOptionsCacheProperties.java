package org.everytldr.enricher.enrichment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.cache.category-options")
public record EnricherCategoryOptionsCacheProperties(Duration ttl) {
  public EnricherCategoryOptionsCacheProperties {
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("ttl must be positive");
    }
  }
}

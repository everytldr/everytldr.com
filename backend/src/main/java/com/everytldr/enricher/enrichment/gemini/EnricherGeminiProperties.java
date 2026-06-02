package com.everytldr.enricher.enrichment.gemini;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.ai.gemini")
public record EnricherGeminiProperties(
    boolean enabled,
    String baseUrl,
    String apiKey,
    String model,
    Duration requestTimeout,
    String promptResource) {
  public EnricherGeminiProperties {
    if (enabled) {
      requireText(baseUrl, "baseUrl");
      requireText(apiKey, "apiKey");
      requireText(model, "model");
      requireText(promptResource, "promptResource");
      if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
        throw new IllegalArgumentException("requestTimeout must be positive");
      }
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
  }
}

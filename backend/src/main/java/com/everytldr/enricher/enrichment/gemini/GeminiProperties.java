package com.everytldr.enricher.enrichment.gemini;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.ai.gemini")
public record GeminiProperties(
    boolean enabled,
    String baseUrl,
    String apiKey,
    String model,
    Duration timeout,
    String promptPath) {
  public GeminiProperties {
    if (enabled) {
      requireText(baseUrl, "baseUrl");
      requireText(apiKey, "apiKey");
      requireText(model, "model");
      requireText(promptPath, "promptPath");
      if (timeout == null || timeout.isZero() || timeout.isNegative()) {
        throw new IllegalArgumentException("timeout must be positive");
      }
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
  }
}

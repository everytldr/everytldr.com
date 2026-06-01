package org.everytldr.enricher.enrichment;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.enricher.content")
public record EnricherContentProperties(
    List<String> allowedHosts,
    Duration requestTimeout,
    int maxRedirects,
    int maxBodyBytes,
    int minBodyChars) {

  public EnricherContentProperties {
    if (allowedHosts == null || allowedHosts.isEmpty()) {
      throw new IllegalArgumentException("allowedHosts must not be empty");
    }
    allowedHosts = normalizeAllowedHosts(allowedHosts);

    if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
      throw new IllegalArgumentException("requestTimeout must be positive");
    }
    if (maxRedirects < 0) {
      throw new IllegalArgumentException("maxRedirects must be zero or positive");
    }
    if (maxBodyBytes <= 0 || maxBodyBytes == Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "maxBodyBytes must be positive and below Integer.MAX_VALUE");
    }
    if (minBodyChars <= 0) {
      throw new IllegalArgumentException("minBodyChars must be positive");
    }
  }

  public boolean isAllowedHost(String host) {
    if (host == null || host.isBlank()) {
      return false;
    }
    return allowedHosts.contains(normalizeHost(host));
  }

  private static List<String> normalizeAllowedHosts(List<String> allowedHosts) {
    List<String> normalizedHosts =
        allowedHosts.stream()
            .filter(Objects::nonNull)
            .map(EnricherContentProperties::normalizeHost)
            .filter(host -> !host.isBlank())
            .distinct()
            .toList();

    if (normalizedHosts.isEmpty()) {
      throw new IllegalArgumentException("allowedHosts must not be empty");
    }
    return List.copyOf(normalizedHosts);
  }

  private static String normalizeHost(String host) {
    String normalized = host.trim().toLowerCase(Locale.ROOT);
    if (normalized.endsWith(".")) {
      return normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}

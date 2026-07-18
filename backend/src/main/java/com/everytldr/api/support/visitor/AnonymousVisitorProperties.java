package com.everytldr.api.support.visitor;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everytldr.visitor")
public record AnonymousVisitorProperties(
    String cookieName, Duration cookieMaxAge, boolean cookieSecure, String hashSecret) {
  public AnonymousVisitorProperties {
    if (cookieName == null || cookieName.isBlank()) {
      throw new IllegalArgumentException("cookieName must not be blank");
    }
    if (cookieMaxAge == null || cookieMaxAge.isZero() || cookieMaxAge.isNegative()) {
      throw new IllegalArgumentException("cookieMaxAge must be positive");
    }
    if (hashSecret == null || hashSecret.isBlank()) {
      throw new IllegalArgumentException("hashSecret must not be blank");
    }
  }
}

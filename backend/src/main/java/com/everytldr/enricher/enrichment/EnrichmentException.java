package com.everytldr.enricher.enrichment;

public class EnrichmentException extends RuntimeException {
  private final boolean retryable;

  private EnrichmentException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
  }

  public static EnrichmentException retryable(String message) {
    return new EnrichmentException(message, null, true);
  }

  public static EnrichmentException retryable(String message, Throwable cause) {
    return new EnrichmentException(message, cause, true);
  }

  public static EnrichmentException permanent(String message) {
    return new EnrichmentException(message, null, false);
  }

  public static EnrichmentException permanent(String message, Throwable cause) {
    return new EnrichmentException(message, cause, false);
  }

  public boolean isRetryable() {
    return retryable;
  }
}

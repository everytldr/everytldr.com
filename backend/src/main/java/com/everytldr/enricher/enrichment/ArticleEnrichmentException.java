package com.everytldr.enricher.enrichment;

/**
 * Classifies enrichment failures as retryable or permanent for processor retry decisions.
 *
 * <p>Retryable failures may be scheduled for another attempt while permanent failures become
 * terminal.
 */
public class ArticleEnrichmentException extends RuntimeException {
  private final boolean retryable;

  private ArticleEnrichmentException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
  }

  public static ArticleEnrichmentException retryable(String message) {
    return new ArticleEnrichmentException(message, null, true);
  }

  public static ArticleEnrichmentException retryable(String message, Throwable cause) {
    return new ArticleEnrichmentException(message, cause, true);
  }

  public static ArticleEnrichmentException permanent(String message) {
    return new ArticleEnrichmentException(message, null, false);
  }

  public static ArticleEnrichmentException permanent(String message, Throwable cause) {
    return new ArticleEnrichmentException(message, cause, false);
  }

  public boolean isRetryable() {
    return retryable;
  }
}

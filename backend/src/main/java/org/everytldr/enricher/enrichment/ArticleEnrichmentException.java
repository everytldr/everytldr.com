package org.everytldr.enricher.enrichment;

/**
 * Enrichment failure를 retryable/permanent로 분류해 processor의 retry decision을 명확히 한다.
 *
 * <p>retryable이면 remaining attempts가 있을 때 retry로 예약되고, permanent이면 terminal failure로 끝난다.
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

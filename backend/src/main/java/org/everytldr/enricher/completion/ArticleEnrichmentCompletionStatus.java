package org.everytldr.enricher.completion;

/** Completion layer의 DB write/state transition 결과. */
public enum ArticleEnrichmentCompletionStatus {
  /** Enrichment result 저장 후 job이 SUCCEEDED로 mark된 상태. */
  SUCCEEDED,

  /** Validation failure 또는 explicit fail 처리로 job이 FAILED로 mark된 상태. */
  FAILED,

  /** Retryable failure 처리로 next attempt가 예약된 상태. */
  RETRY_SCHEDULED,

  /** Job이 PROCESSING 상태가 아니어서 DB write/state transition을 skip한 상태. */
  SKIPPED_NOT_PROCESSING
}

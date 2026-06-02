package com.everytldr.enricher.completion;

/** Result of completion-layer DB writes and job state transitions. */
public enum ArticleEnrichmentCompletionStatus {
  /** Enrichment result was saved and the job was marked SUCCEEDED. */
  SUCCEEDED,

  /** Validation or explicit failure marked the job FAILED. */
  FAILED,

  /** Retryable failure scheduled the next attempt. */
  RETRY_SCHEDULED,

  /** Job was no longer PROCESSING, so DB writes and state transition were skipped. */
  SKIPPED_NOT_PROCESSING
}

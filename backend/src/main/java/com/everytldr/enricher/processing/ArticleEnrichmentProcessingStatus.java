package com.everytldr.enricher.processing;

/** Final orchestration outcome for a claimed enrichment job. */
public enum ArticleEnrichmentProcessingStatus {
  /** Enrichment finished and the job was marked SUCCEEDED. */
  SUCCEEDED,

  /** Processing ended in terminal failure without retry scheduling. */
  FAILED,

  /** Retryable failure occurred and the next attempt was scheduled. */
  RETRY_SCHEDULED,

  /** Job existed but was no longer PROCESSING, so processing was skipped. */
  SKIPPED_NOT_PROCESSING,

  /** Claimed job id was no longer found in the database, so processing was skipped. */
  SKIPPED_NOT_FOUND
}

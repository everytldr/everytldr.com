package com.everytldr.enricher.processing;

public enum ArticleEnrichmentProcessingStatus {
  SUCCEEDED,

  FAILED,

  RETRY_SCHEDULED,

  SKIPPED_NOT_PROCESSING,

  SKIPPED_NOT_FOUND
}

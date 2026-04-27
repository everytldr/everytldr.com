package org.tldrtimes.common.domain.ingestion;

public enum IngestionState {
  PENDING,
  PROCESSING,
  SUCCEEDED,
  FAILED,
  RETRY_SCHEDULED
}

package org.everytldr.common.domain.ingestion;

public enum IngestionState {
  PENDING,
  PROCESSING,
  SUCCEEDED,
  FAILED,
  RETRY_SCHEDULED
}

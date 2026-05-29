package org.everytldr.enricher.processing;

/** Processing layer에서 claimed job 처리 orchestration의 최종 outcome. */
public enum ArticleEnrichmentProcessingStatus {
  /** Enrichment 완료 후 job이 SUCCEEDED로 mark된 상태. */
  SUCCEEDED,

  /** 재시도 없이 terminal failure로 끝난 상태. */
  FAILED,

  /** Retryable failure가 발생했고 next attempt가 예약된 상태. */
  RETRY_SCHEDULED,

  /** Job은 존재하지만 더 이상 PROCESSING 상태가 아니어서 skip된 상태. */
  SKIPPED_NOT_PROCESSING,

  /** Claimed job id가 DB에서 더 이상 조회되지 않아 skip된 상태. */
  SKIPPED_NOT_FOUND
}

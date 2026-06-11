package com.everytldr.enricher.processor;

import com.everytldr.enricher.completion.CompletionStatus;

public record ProcessingResult(Long jobId, Status status) {
  public static ProcessingResult from(Long jobId, CompletionStatus status) {
    return new ProcessingResult(jobId, Status.from(status));
  }

  public enum Status {
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    SKIPPED_NOT_PROCESSING,
    SKIPPED_NOT_FOUND;

    private static Status from(CompletionStatus status) {
      return switch (status) {
        case SUCCEEDED -> SUCCEEDED;
        case FAILED -> FAILED;
        case RETRY_SCHEDULED -> RETRY_SCHEDULED;
        case SKIPPED_NOT_PROCESSING -> SKIPPED_NOT_PROCESSING;
      };
    }
  }
}

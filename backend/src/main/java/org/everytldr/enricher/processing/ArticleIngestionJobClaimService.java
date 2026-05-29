package org.everytldr.enricher.processing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.everytldr.common.domain.ingestion.ArticleIngestionJob;
import org.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class ArticleIngestionJobClaimService {

  private final ArticleIngestionJobRepository articleIngestionJobRepository;
  private final EnricherProcessingProperties properties;

  @Transactional
  public List<Long> claimNextJobs(Instant now, int limit) {
    Objects.requireNonNull(now, "now must not be null");
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be positive");
    }

    List<Long> claimedJobIds = new ArrayList<>();
    recoverStaleProcessingJobs(now, limit, claimedJobIds);

    int remainingLimit = limit - claimedJobIds.size();
    if (remainingLimit < 1) {
      return claimedJobIds;
    }

    List<ArticleIngestionJob> jobs =
        articleIngestionJobRepository.findClaimableJobsForUpdate(now, remainingLimit);
    for (ArticleIngestionJob job : jobs) {
      if (job.claimForAttempt(now)) {
        claimedJobIds.add(job.getId());
      }
    }
    return claimedJobIds;
  }

  private void recoverStaleProcessingJobs(Instant now, int limit, List<Long> claimedJobIds) {
    Instant staleAttemptStartedAtOrBefore = now.minus(properties.staleTimeout());
    List<ArticleIngestionJob> staleJobs =
        articleIngestionJobRepository.findStaleProcessingJobsForUpdate(
            staleAttemptStartedAtOrBefore, limit);

    for (ArticleIngestionJob job : staleJobs) {
      if (!job.isStaleProcessing(now, properties.staleTimeout())) {
        continue;
      }

      if (job.getAttemptCount() >= properties.maxAttempts()) {
        job.markFailed("max attempts exhausted after stale processing timeout");
        continue;
      }

      if (job.reclaimStaleProcessingAttempt(now, properties.staleTimeout())) {
        claimedJobIds.add(job.getId());
      }
    }
  }
}

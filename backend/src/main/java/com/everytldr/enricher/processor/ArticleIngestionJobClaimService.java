package com.everytldr.enricher.processor;

import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class ArticleIngestionJobClaimService {

  private final ArticleIngestionJobRepository articleIngestionJobRepository;
  private final ProcessingProperties properties;

  @Transactional
  public List<ArticleIngestionJob> claimNextJobs(Instant now, int limit) {
    Objects.requireNonNull(now, "now must not be null");
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be positive");
    }

    List<ArticleIngestionJob> claimedStaleJobs = recoverStaleJobs(now, limit);

    int remainingClaimLimit = limit - claimedStaleJobs.size();
    if (remainingClaimLimit < 1) {
      return claimedStaleJobs;
    }

    List<ArticleIngestionJob> claimedAvailableJobs = claimAvailableJobs(now, remainingClaimLimit);
    List<ArticleIngestionJob> claimedJobs = new ArrayList<>(claimedStaleJobs);
    claimedJobs.addAll(claimedAvailableJobs);
    return claimedJobs;
  }

  private List<ArticleIngestionJob> claimAvailableJobs(Instant now, int limit) {
    List<ArticleIngestionJob> jobs =
        articleIngestionJobRepository.findClaimableJobsForUpdate(now, limit);
    List<ArticleIngestionJob> claimedJobs = new ArrayList<>();
    for (ArticleIngestionJob job : jobs) {
      boolean claimed = job.claimForAttempt(now);
      if (claimed) {
        claimedJobs.add(job);
      }
    }
    return claimedJobs;
  }

  private List<ArticleIngestionJob> recoverStaleJobs(Instant now, int limit) {
    Duration staleTimeout = properties.staleTimeout();
    Instant staleThreshold = now.minus(staleTimeout);
    List<ArticleIngestionJob> staleJobs =
        articleIngestionJobRepository.findStaleProcessingJobsForUpdate(staleThreshold, limit);

    List<ArticleIngestionJob> claimedJobs = new ArrayList<>();
    for (ArticleIngestionJob job : staleJobs) {
      recoverStaleJob(job, now, staleTimeout).ifPresent(claimedJobs::add);
    }
    return claimedJobs;
  }

  private Optional<ArticleIngestionJob> recoverStaleJob(
      ArticleIngestionJob job, Instant now, Duration staleTimeout) {
    if (!job.isStaleProcessing(now, staleTimeout)) {
      return Optional.empty();
    }

    if (job.getAttemptCount() >= properties.maxAttempts()) {
      job.markFailed("max attempts exhausted after stale processing timeout");
      return Optional.empty();
    }

    if (job.reclaimStaleProcessingAttempt(now, staleTimeout)) {
      return Optional.of(job);
    }

    return Optional.empty();
  }
}

package org.everytldr.enricher.enrichment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.everytldr.common.domain.ingestion.ArticleIngestionJob;
import org.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class ArticleIngestionJobClaimService {

  private final ArticleIngestionJobRepository articleIngestionJobRepository;

  @Transactional
  public List<Long> claimNextJobs(Instant now, int limit) {
    List<ArticleIngestionJob> jobs =
        articleIngestionJobRepository.findClaimableJobsForUpdate(now, limit);

    List<Long> claimedJobIds = new ArrayList<>();
    for (ArticleIngestionJob job : jobs) {
      if (job.claimForAttempt(now)) {
        claimedJobIds.add(job.getId());
      }
    }
    return claimedJobIds;
  }
}

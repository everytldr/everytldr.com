package org.everytldr.enricher.processing;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import org.everytldr.TestcontainersConfig;
import org.everytldr.common.domain.article.Article;
import org.everytldr.common.domain.article.ArticleRepository;
import org.everytldr.common.domain.ingestion.ArticleIngestionJob;
import org.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import org.everytldr.common.domain.ingestion.IngestionState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles({"test", "enricher"})
@Transactional
class ArticleIngestionJobClaimServiceTest {

  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");

  @Autowired private ArticleIngestionJobClaimService articleIngestionJobClaimService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleIngestionJobRepository articleIngestionJobRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void claimsPendingAndDueRetryJobs() {
    ArticleIngestionJob pendingJob = savePendingJob("https://example.com/enricher/pending");
    ArticleIngestionJob dueRetryJob =
        saveRetryScheduledJob("https://example.com/enricher/due-retry", NOW);
    ArticleIngestionJob futureRetryJob =
        saveRetryScheduledJob("https://example.com/enricher/future-retry", NOW.plusSeconds(60));
    ArticleIngestionJob processingJob =
        saveProcessingJob("https://example.com/enricher/processing");
    ArticleIngestionJob succeededJob = saveSucceededJob("https://example.com/enricher/succeeded");
    ArticleIngestionJob failedJob = saveFailedJob("https://example.com/enricher/failed");
    flushAndClear();

    List<Long> claimedJobIds = articleIngestionJobClaimService.claimNextJobs(NOW, 10);
    flushAndClear();

    assertThat(claimedJobIds).containsExactlyInAnyOrder(pendingJob.getId(), dueRetryJob.getId());
    assertClaimed(pendingJob.getId(), 1);
    assertClaimed(dueRetryJob.getId(), 2);
    assertState(futureRetryJob.getId(), IngestionState.RETRY_SCHEDULED);
    assertState(processingJob.getId(), IngestionState.PROCESSING);
    assertState(succeededJob.getId(), IngestionState.SUCCEEDED);
    assertState(failedJob.getId(), IngestionState.FAILED);
  }

  @Test
  void limitsClaimedJobs() {
    savePendingJob("https://example.com/enricher/limit-1");
    savePendingJob("https://example.com/enricher/limit-2");
    savePendingJob("https://example.com/enricher/limit-3");
    flushAndClear();

    List<Long> claimedJobIds = articleIngestionJobClaimService.claimNextJobs(NOW, 2);

    assertThat(claimedJobIds).hasSize(2);
  }

  private void assertClaimed(Long jobId, int expectedAttemptCount) {
    ArticleIngestionJob job = articleIngestionJobRepository.findById(jobId).orElseThrow();

    assertThat(job.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(job.getAttemptCount()).isEqualTo(expectedAttemptCount);
    assertThat(job.getAttemptStartedAt()).isEqualTo(NOW);
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  private void assertState(Long jobId, IngestionState expectedState) {
    assertThat(articleIngestionJobRepository.findById(jobId).orElseThrow().getState())
        .isEqualTo(expectedState);
  }

  private ArticleIngestionJob savePendingJob(String sourceUrl) {
    Article article =
        articleRepository.saveAndFlush(
            Article.create(sourceUrl, "Example Source", null, "en", PUBLISHED_AT));
    ArticleIngestionJob job = ArticleIngestionJob.create(article, sha256(sourceUrl));
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob saveRetryScheduledJob(String sourceUrl, Instant nextAttemptAt) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl);
    job.scheduleRetry(nextAttemptAt, "temporary failure");
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob saveProcessingJob(String sourceUrl) {
    ArticleIngestionJob job = savePendingJob(sourceUrl);
    boolean claimed = job.claimForAttempt(NOW.minusSeconds(60));
    assertThat(claimed).isTrue();
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob saveSucceededJob(String sourceUrl) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl);
    job.markSucceeded();
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob saveFailedJob(String sourceUrl) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl);
    job.markFailed("permanent failure");
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private byte[] sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}

package org.everytldr.common.domain.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import org.everytldr.TestcontainersConfig;
import org.everytldr.common.domain.article.Article;
import org.everytldr.common.domain.article.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class ArticleIngestionJobRepositoryTest {

  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleIngestionJobRepository articleIngestionJobRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void findsPendingAndDueRetryJobsForUpdate() {
    ArticleIngestionJob pendingJob = savePendingJob("https://example.com/pending");
    ArticleIngestionJob dueRetryJob = saveRetryScheduledJob("https://example.com/due-retry", NOW);
    ArticleIngestionJob futureRetryJob =
        saveRetryScheduledJob("https://example.com/future-retry", NOW.plusSeconds(60));
    ArticleIngestionJob processingJob = saveProcessingJob("https://example.com/processing");
    ArticleIngestionJob succeededJob = saveSucceededJob("https://example.com/succeeded");
    ArticleIngestionJob failedJob = saveFailedJob("https://example.com/failed");
    flushAndClear();

    List<ArticleIngestionJob> claimableJobs =
        articleIngestionJobRepository.findClaimableJobsForUpdate(NOW, 10);

    assertThat(claimableJobs)
        .extracting(ArticleIngestionJob::getId)
        .contains(pendingJob.getId(), dueRetryJob.getId())
        .doesNotContain(
            futureRetryJob.getId(), processingJob.getId(), succeededJob.getId(), failedJob.getId());
  }

  @Test
  void limitsClaimableJobsForUpdate() {
    savePendingJob("https://example.com/limit-1");
    savePendingJob("https://example.com/limit-2");
    savePendingJob("https://example.com/limit-3");
    flushAndClear();

    List<ArticleIngestionJob> claimableJobs =
        articleIngestionJobRepository.findClaimableJobsForUpdate(NOW, 2);

    assertThat(claimableJobs).hasSize(2);
  }

  @Test
  void claimableJobLookupRequiresNow() {
    assertThatNullPointerException()
        .isThrownBy(() -> articleIngestionJobRepository.findClaimableJobsForUpdate(null, 1))
        .withMessage("now must not be null");
  }

  @Test
  void claimableJobLookupRequiresPositiveLimit() {
    assertThatThrownBy(() -> articleIngestionJobRepository.findClaimableJobsForUpdate(NOW, 0))
        .hasMessageContaining("limit must be positive");
  }

  private ArticleIngestionJob savePendingJob(String sourceUrl) {
    Article article =
        articleRepository.save(
            Article.create(sourceUrl, "Example Source", null, "en", PUBLISHED_AT));
    ArticleIngestionJob job = ArticleIngestionJob.create(article, sha256(sourceUrl));
    return articleIngestionJobRepository.save(job);
  }

  private ArticleIngestionJob saveRetryScheduledJob(String sourceUrl, Instant nextAttemptAt) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl);
    job.scheduleRetry(nextAttemptAt, "temporary failure");
    return job;
  }

  private ArticleIngestionJob saveProcessingJob(String sourceUrl) {
    ArticleIngestionJob job = savePendingJob(sourceUrl);
    boolean claimed = job.claimForAttempt(NOW.minusSeconds(60));
    assertThat(claimed).isTrue();
    return job;
  }

  private ArticleIngestionJob saveSucceededJob(String sourceUrl) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl);
    job.markSucceeded();
    return job;
  }

  private ArticleIngestionJob saveFailedJob(String sourceUrl) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl);
    job.markFailed("permanent failure");
    return job;
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

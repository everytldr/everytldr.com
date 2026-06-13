package com.everytldr.common.domain.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
  private static final Duration STALE_TIMEOUT = Duration.ofMinutes(15);

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleIngestionJobRepository articleIngestionJobRepository;

  @Autowired private ArticleSourceRepository articleSourceRepository;

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
  void findsStaleProcessingJobsForUpdate() {
    Instant staleAttemptStartedAt = NOW.minus(STALE_TIMEOUT).minusSeconds(1);
    Instant staleBoundaryAttemptStartedAt = NOW.minus(STALE_TIMEOUT);
    Instant freshAttemptStartedAt = NOW.minus(STALE_TIMEOUT).plusSeconds(1);
    ArticleIngestionJob staleJob =
        saveProcessingJob("https://example.com/stale-processing", staleAttemptStartedAt);
    ArticleIngestionJob boundaryStaleJob =
        saveProcessingJob(
            "https://example.com/boundary-stale-processing", staleBoundaryAttemptStartedAt);
    ArticleIngestionJob freshJob =
        saveProcessingJob("https://example.com/fresh-processing", freshAttemptStartedAt);
    ArticleIngestionJob pendingJob = savePendingJob("https://example.com/pending-not-stale");
    ArticleIngestionJob dueRetryJob =
        saveRetryScheduledJob("https://example.com/due-retry-not-stale", NOW);
    ArticleIngestionJob succeededJob = saveSucceededJob("https://example.com/succeeded-not-stale");
    ArticleIngestionJob failedJob = saveFailedJob("https://example.com/failed-not-stale");
    flushAndClear();

    List<ArticleIngestionJob> staleJobs =
        articleIngestionJobRepository.findStaleProcessingJobsForUpdate(
            NOW.minus(STALE_TIMEOUT), 10);

    assertThat(staleJobs)
        .extracting(ArticleIngestionJob::getId)
        .contains(staleJob.getId(), boundaryStaleJob.getId())
        .doesNotContain(
            freshJob.getId(),
            pendingJob.getId(),
            dueRetryJob.getId(),
            succeededJob.getId(),
            failedJob.getId());
  }

  @Test
  void limitsStaleProcessingJobsForUpdate() {
    saveProcessingJob(
        "https://example.com/stale-limit-1", NOW.minus(STALE_TIMEOUT).minusSeconds(3));
    saveProcessingJob(
        "https://example.com/stale-limit-2", NOW.minus(STALE_TIMEOUT).minusSeconds(2));
    saveProcessingJob(
        "https://example.com/stale-limit-3", NOW.minus(STALE_TIMEOUT).minusSeconds(1));
    flushAndClear();

    List<ArticleIngestionJob> staleJobs =
        articleIngestionJobRepository.findStaleProcessingJobsForUpdate(NOW.minus(STALE_TIMEOUT), 2);

    assertThat(staleJobs).hasSize(2);
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

  @Test
  void staleProcessingLookupRequiresCutoff() {
    assertThatNullPointerException()
        .isThrownBy(() -> articleIngestionJobRepository.findStaleProcessingJobsForUpdate(null, 1))
        .withMessage("staleThreshold must not be null");
  }

  @Test
  void staleProcessingLookupRequiresPositiveLimit() {
    assertThatThrownBy(
            () ->
                articleIngestionJobRepository.findStaleProcessingJobsForUpdate(
                    NOW.minus(STALE_TIMEOUT), 0))
        .hasMessageContaining("limit must be positive");
  }

  private ArticleIngestionJob savePendingJob(String sourceUrl) {
    source();
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
    return saveProcessingJob(sourceUrl, NOW.minusSeconds(60));
  }

  private ArticleIngestionJob saveProcessingJob(String sourceUrl, Instant attemptStartedAt) {
    ArticleIngestionJob job = savePendingJob(sourceUrl);
    boolean claimed = job.claimForAttempt(attemptStartedAt);
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

  private ArticleSource source() {
    return articleSourceRepository
        .findByName("Example Source")
        .orElseGet(
            () ->
                articleSourceRepository.saveAndFlush(
                    ArticleSource.create(
                        "Example Source",
                        "https://example.com/feed.xml",
                        new SourcePolicy(
                            new CrawlingPolicy(
                                List.of("example.com"), List.of("article"), List.of())),
                        "en",
                        SourceType.RSS)));
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

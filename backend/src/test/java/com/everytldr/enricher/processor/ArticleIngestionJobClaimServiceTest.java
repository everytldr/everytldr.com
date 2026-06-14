package com.everytldr.enricher.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
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
@ActiveProfiles({"test", "enricher"})
@Transactional
class ArticleIngestionJobClaimServiceTest {

  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");
  private static final Duration STALE_TIMEOUT = Duration.ofMinutes(15);

  @Autowired private ArticleIngestionJobClaimService claimService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleIngestionJobRepository jobRepository;
  @Autowired private ArticleSourceRepository sourceRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void claimsPendingAndDueRetryJobsOnly() {
    ArticleIngestionJob pendingJob = savePendingJob("https://example.com/enricher/pending");
    ArticleIngestionJob dueRetryJob =
        saveRetryScheduledJob("https://example.com/enricher/due-retry", NOW);
    ArticleIngestionJob futureRetryJob =
        saveRetryScheduledJob("https://example.com/enricher/future-retry", NOW.plusSeconds(60));
    ArticleIngestionJob succeededJob = saveSucceededJob("https://example.com/enricher/succeeded");
    flushAndClear();

    List<Long> claimedJobIds =
        claimService.claimNextJobs(NOW, 10).stream().map(ArticleIngestionJob::getId).toList();
    flushAndClear();

    assertThat(claimedJobIds).containsExactlyInAnyOrder(pendingJob.getId(), dueRetryJob.getId());
    assertClaimed(pendingJob.getId(), 1);
    assertClaimed(dueRetryJob.getId(), 2);
    assertState(futureRetryJob.getId(), IngestionState.RETRY_SCHEDULED);
    assertState(succeededJob.getId(), IngestionState.SUCCEEDED);
  }

  @Test
  void recoversStaleProcessingJobsBeforeClaimingPendingJobs() {
    ArticleIngestionJob staleJob =
        saveProcessingJob(
            "https://example.com/enricher/stale", NOW.minus(STALE_TIMEOUT).minusSeconds(1), 1);
    ArticleIngestionJob pendingJob =
        savePendingJob("https://example.com/enricher/pending-after-stale");
    flushAndClear();

    List<Long> claimedJobIds =
        claimService.claimNextJobs(NOW, 2).stream().map(ArticleIngestionJob::getId).toList();
    flushAndClear();

    assertThat(claimedJobIds).containsExactly(staleJob.getId(), pendingJob.getId());
    assertClaimed(staleJob.getId(), 2);
    assertClaimed(pendingJob.getId(), 1);
  }

  @Test
  void failsStaleProcessingJobWhenMaxAttemptsAreExhausted() {
    ArticleIngestionJob staleJob =
        saveProcessingJob(
            "https://example.com/enricher/stale-max-attempts",
            NOW.minus(STALE_TIMEOUT).minusSeconds(1),
            3);
    flushAndClear();

    List<ArticleIngestionJob> claimedJobs = claimService.claimNextJobs(NOW, 1);
    flushAndClear();

    ArticleIngestionJob reloadedJob = jobRepository.findById(staleJob.getId()).orElseThrow();
    assertThat(claimedJobs).isEmpty();
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getAttemptCount()).isEqualTo(3);
    assertThat(reloadedJob.getLastErrorMessage())
        .isEqualTo("max attempts exhausted after stale processing timeout");
  }

  private void assertClaimed(Long jobId, int expectedAttemptCount) {
    ArticleIngestionJob job = jobRepository.findById(jobId).orElseThrow();

    assertThat(job.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(job.getAttemptCount()).isEqualTo(expectedAttemptCount);
    assertThat(job.getAttemptStartedAt()).isEqualTo(NOW);
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  private void assertState(Long jobId, IngestionState expectedState) {
    assertThat(jobRepository.findById(jobId).orElseThrow().getState()).isEqualTo(expectedState);
  }

  private ArticleIngestionJob savePendingJob(String sourceUrl) {
    source();
    Article article =
        articleRepository.saveAndFlush(
            Article.create(sourceUrl, "Example Source", null, "en", PUBLISHED_AT));
    return jobRepository.saveAndFlush(ArticleIngestionJob.create(article, sha256(sourceUrl)));
  }

  private ArticleIngestionJob saveRetryScheduledJob(String sourceUrl, Instant nextAttemptAt) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl, NOW.minusSeconds(60), 1);
    job.scheduleRetry(nextAttemptAt, "temporary failure");
    return jobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob saveSucceededJob(String sourceUrl) {
    ArticleIngestionJob job = saveProcessingJob(sourceUrl, NOW.minusSeconds(60), 1);
    job.markSucceeded();
    return jobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob saveProcessingJob(
      String sourceUrl, Instant finalAttemptStartedAt, int attemptCount) {
    ArticleIngestionJob job = savePendingJob(sourceUrl);
    for (int attempt = 1; attempt <= attemptCount; attempt++) {
      Instant attemptStartedAt =
          finalAttemptStartedAt.minusSeconds((long) (attemptCount - attempt) * 60);
      assertThat(job.claimForAttempt(attemptStartedAt)).isTrue();
      if (attempt < attemptCount) {
        job.scheduleRetry(attemptStartedAt, "temporary failure");
      }
    }
    return jobRepository.saveAndFlush(job);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private ArticleSource source() {
    return sourceRepository
        .findByName("Example Source")
        .orElseGet(
            () ->
                sourceRepository.saveAndFlush(
                    ArticleSource.create(
                        "Example Source",
                        new SourcePolicy(
                            new CrawlingPolicy(
                                List.of("https://example.com/feed.xml"),
                                List.of("example.com"),
                                List.of("article"),
                                List.of())),
                        "en",
                        SourceType.RSS)));
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}

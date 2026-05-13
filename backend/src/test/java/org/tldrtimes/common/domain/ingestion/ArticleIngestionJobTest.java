package org.tldrtimes.common.domain.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.tldrtimes.common.domain.article.Article;

class ArticleIngestionJobTest {

  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");

  @Test
  void createsPendingJobWithNoAttempts() {
    ArticleIngestionJob job = newJob("https://www.theguardian.com/football/new");

    assertThat(job.getState()).isEqualTo(IngestionState.PENDING);
    assertThat(job.getAttemptCount()).isZero();
    assertThat(job.getAttemptStartedAt()).isNull();
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  @Test
  void pendingJobCanBeClaimedForFirstAttempt() {
    ArticleIngestionJob job = newJob("https://www.theguardian.com/football/claim");
    Instant now = Instant.parse("2026-05-13T01:00:00Z");

    boolean claimed = job.claimForAttempt(now);

    assertThat(claimed).isTrue();
    assertThat(job.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(job.getAttemptCount()).isEqualTo(1);
    assertThat(job.getAttemptStartedAt()).isEqualTo(now);
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  @Test
  void retryScheduledJobCanBeClaimedWhenNextAttemptTimeHasPassed() {
    ArticleIngestionJob job = processingJob();
    Instant nextAttemptAt = Instant.parse("2026-05-13T01:00:00Z");
    job.scheduleRetry(nextAttemptAt, "temporary failure");

    boolean claimed = job.claimForAttempt(nextAttemptAt.plusSeconds(1));

    assertThat(claimed).isTrue();
    assertThat(job.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(job.getAttemptCount()).isEqualTo(2);
    assertThat(job.getAttemptStartedAt()).isEqualTo(nextAttemptAt.plusSeconds(1));
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  @Test
  void retryScheduledJobCannotBeClaimedBeforeNextAttemptTime() {
    ArticleIngestionJob job = processingJob();
    Instant nextAttemptAt = Instant.parse("2026-05-13T01:00:00Z");
    job.scheduleRetry(nextAttemptAt, "temporary failure");

    boolean claimed = job.claimForAttempt(nextAttemptAt.minusSeconds(1));

    assertThat(claimed).isFalse();
    assertThat(job.getState()).isEqualTo(IngestionState.RETRY_SCHEDULED);
    assertThat(job.getAttemptCount()).isEqualTo(1);
    assertThat(job.getAttemptStartedAt()).isNull();
    assertThat(job.getNextAttemptAt()).isEqualTo(nextAttemptAt);
    assertThat(job.getLastErrorMessage()).isEqualTo("temporary failure");
  }

  @Test
  void retryScheduledJobCanBeClaimedAtNextAttemptTime() {
    ArticleIngestionJob job = processingJob();
    Instant nextAttemptAt = Instant.parse("2026-05-13T01:00:00Z");
    job.scheduleRetry(nextAttemptAt, "temporary failure");

    boolean claimed = job.claimForAttempt(nextAttemptAt);

    assertThat(claimed).isTrue();
    assertThat(job.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(job.getAttemptCount()).isEqualTo(2);
    assertThat(job.getAttemptStartedAt()).isEqualTo(nextAttemptAt);
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  @Test
  void processingJobCanBeMarkedSucceeded() {
    ArticleIngestionJob job = processingJob();

    job.markSucceeded();

    assertThat(job.getState()).isEqualTo(IngestionState.SUCCEEDED);
    assertThat(job.getAttemptCount()).isEqualTo(1);
    assertThat(job.getAttemptStartedAt()).isNull();
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isNull();
  }

  @Test
  void processingJobCanBeScheduledForRetry() {
    ArticleIngestionJob job = processingJob();
    Instant nextAttemptAt = Instant.parse("2026-05-13T01:10:00Z");

    job.scheduleRetry(nextAttemptAt, "provider timeout");

    assertThat(job.getState()).isEqualTo(IngestionState.RETRY_SCHEDULED);
    assertThat(job.getAttemptCount()).isEqualTo(1);
    assertThat(job.getAttemptStartedAt()).isNull();
    assertThat(job.getNextAttemptAt()).isEqualTo(nextAttemptAt);
    assertThat(job.getLastErrorMessage()).isEqualTo("provider timeout");
  }

  @Test
  void schedulingRetryRequiresNextAttemptTime() {
    ArticleIngestionJob job = processingJob();

    assertThatNullPointerException()
        .isThrownBy(() -> job.scheduleRetry(null, "provider timeout"))
        .withMessage("nextAttemptAt must not be null");
  }

  @Test
  void processingJobCanBeMarkedFailed() {
    ArticleIngestionJob job = processingJob();

    job.markFailed("invalid enrichment response");

    assertThat(job.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(job.getAttemptCount()).isEqualTo(1);
    assertThat(job.getAttemptStartedAt()).isNull();
    assertThat(job.getNextAttemptAt()).isNull();
    assertThat(job.getLastErrorMessage()).isEqualTo("invalid enrichment response");
  }

  @Test
  void lastErrorMessageIsTruncatedToColumnLimit() {
    ArticleIngestionJob job = processingJob();
    String errorMessage = "x".repeat(1001);

    job.markFailed(errorMessage);

    assertThat(job.getLastErrorMessage()).hasSize(1000);
  }

  private ArticleIngestionJob processingJob() {
    ArticleIngestionJob job = newJob("https://www.theguardian.com/football/processing");
    boolean claimed = job.claimForAttempt(Instant.parse("2026-05-13T00:00:00Z"));
    assertThat(claimed).isTrue();
    return job;
  }

  private ArticleIngestionJob newJob(String sourceUrl) {
    Article article = Article.create(sourceUrl, "The Guardian Football", null, "en", PUBLISHED_AT);
    return ArticleIngestionJob.create(article, sha256(sourceUrl));
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

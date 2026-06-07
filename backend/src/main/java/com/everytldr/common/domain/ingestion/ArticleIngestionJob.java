package com.everytldr.common.domain.ingestion;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    indexes = {
      @Index(
          name = "idx_article_ingestion_job_state_next_attempt_at",
          columnList = "state, next_attempt_at"),
      @Index(
          name = "idx_article_ingestion_job_state_attempt_started_at",
          columnList = "state, attempt_started_at")
    },
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_article_ingestion_job_article", columnNames = "article_id"),
      @UniqueConstraint(name = "uk_article_ingestion_job_url_hash", columnNames = "url_hash")
    })
public class ArticleIngestionJob extends BaseEntity {
  private static final int LAST_ERROR_MESSAGE_MAX_LENGTH = 1000;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_article_ingestion_job_article"))
  private Article article;

  @Column(columnDefinition = "BINARY(32) NOT NULL")
  private byte[] urlHash;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 32)
  private IngestionState state;

  @Column(nullable = false)
  private int attemptCount;

  private Instant attemptStartedAt;

  private Instant nextAttemptAt;

  @Column(length = LAST_ERROR_MESSAGE_MAX_LENGTH)
  private String lastErrorMessage;

  private ArticleIngestionJob(
      Article article,
      byte[] urlHash,
      IngestionState state,
      int attemptCount,
      Instant attemptStartedAt,
      Instant nextAttemptAt,
      String lastErrorMessage) {
    this.article = article;
    this.urlHash = urlHash;
    this.state = state;
    this.attemptCount = attemptCount;
    this.attemptStartedAt = attemptStartedAt;
    this.nextAttemptAt = nextAttemptAt;
    this.lastErrorMessage = lastErrorMessage;
  }

  public static ArticleIngestionJob create(Article article, byte[] urlHash) {
    return new ArticleIngestionJob(article, urlHash, IngestionState.PENDING, 0, null, null, null);
  }

  public boolean claimForAttempt(Instant now) {
    Objects.requireNonNull(now, "now must not be null");
    if (!canClaim(now)) {
      return false;
    }

    this.state = IngestionState.PROCESSING;
    this.attemptCount += 1;
    this.attemptStartedAt = now;
    this.nextAttemptAt = null;
    this.lastErrorMessage = null;
    return true;
  }

  public boolean reclaimStaleProcessingAttempt(Instant now, Duration staleTimeout) {
    if (!isStaleProcessing(now, staleTimeout)) {
      return false;
    }

    this.attemptCount += 1;
    this.attemptStartedAt = now;
    this.nextAttemptAt = null;
    this.lastErrorMessage = null;
    return true;
  }

  public boolean isStaleProcessing(Instant now, Duration staleTimeout) {
    Objects.requireNonNull(now, "now must not be null");
    Objects.requireNonNull(staleTimeout, "staleTimeout must not be null");
    if (staleTimeout.isNegative() || staleTimeout.isZero()) {
      throw new IllegalArgumentException("staleTimeout must be positive");
    }

    return this.state.equals(IngestionState.PROCESSING)
        && this.attemptStartedAt != null
        && !this.attemptStartedAt.plus(staleTimeout).isAfter(now);
  }

  public void markSucceeded() {
    if (this.state.equals(IngestionState.PROCESSING)) {
      this.state = IngestionState.SUCCEEDED;
      this.attemptStartedAt = null;
      this.nextAttemptAt = null;
      this.lastErrorMessage = null;
    }
  }

  public void scheduleRetry(Instant nextAttemptAt, String errorMessage) {
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    if (this.state.equals(IngestionState.PROCESSING)) {
      this.state = IngestionState.RETRY_SCHEDULED;
      this.attemptStartedAt = null;
      this.nextAttemptAt = nextAttemptAt;
      this.lastErrorMessage = truncateLastErrorMessage(errorMessage);
    }
  }

  public void markFailed(String errorMessage) {
    if (this.state.equals(IngestionState.PROCESSING)) {
      this.state = IngestionState.FAILED;
      this.attemptStartedAt = null;
      this.nextAttemptAt = null;
      this.lastErrorMessage = truncateLastErrorMessage(errorMessage);
    }
  }

  private boolean canClaim(Instant now) {
    if (this.state.equals(IngestionState.PENDING)) {
      return true;
    }
    return this.state.equals(IngestionState.RETRY_SCHEDULED)
        && this.nextAttemptAt != null
        && !this.nextAttemptAt.isAfter(now); // nextAttemptAt <= now
  }

  private String truncateLastErrorMessage(String message) {
    if (message == null || message.length() <= LAST_ERROR_MESSAGE_MAX_LENGTH) {
      return message;
    }
    return message.substring(0, LAST_ERROR_MESSAGE_MAX_LENGTH);
  }
}

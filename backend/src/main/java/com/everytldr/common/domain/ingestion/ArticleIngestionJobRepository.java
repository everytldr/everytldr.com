package com.everytldr.common.domain.ingestion;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleIngestionJobRepository extends JpaRepository<ArticleIngestionJob, Long> {
  Optional<ArticleIngestionJob> findByUrlHash(byte[] urlHash);

  @Query(
      """
          SELECT j
          FROM ArticleIngestionJob j
          JOIN FETCH j.article
          WHERE j.id = :id
          """)
  Optional<ArticleIngestionJob> findByIdWithArticle(@Param("id") Long id);

  Optional<ArticleIngestionJob> findByArticleId(Long articleId);

  boolean existsByUrlHash(byte[] urlHash);

  @Query("SELECT j.urlHash FROM ArticleIngestionJob j WHERE j.urlHash IN :urlHashes")
  List<byte[]> findExistingUrlHashes(@Param("urlHashes") Collection<byte[]> urlHashes);

  default List<ArticleIngestionJob> findClaimableJobsForUpdate(Instant now, int limit) {
    Objects.requireNonNull(now, "now must not be null");
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be positive");
    }
    return findClaimableJobsForUpdate(
        IngestionState.PENDING, IngestionState.RETRY_SCHEDULED, now, Limit.of(limit));
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT j
      FROM ArticleIngestionJob j
      WHERE j.state = :pendingState
         OR (j.state = :retryScheduledState AND j.nextAttemptAt <= :now)
      ORDER BY j.nextAttemptAt ASC, j.id ASC
      """)
  List<ArticleIngestionJob> findClaimableJobsForUpdate(
      @Param("pendingState") IngestionState pendingState,
      @Param("retryScheduledState") IngestionState retryScheduledState,
      @Param("now") Instant now,
      Limit limit);

  default List<ArticleIngestionJob> findStaleProcessingJobsForUpdate(
      Instant staleAttemptStartedAtOrBefore, int limit) {
    Objects.requireNonNull(
        staleAttemptStartedAtOrBefore, "staleAttemptStartedAtOrBefore must not be null");
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be positive");
    }
    return findStaleProcessingJobsForUpdate(
        IngestionState.PROCESSING, staleAttemptStartedAtOrBefore, Limit.of(limit));
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT j
      FROM ArticleIngestionJob j
      WHERE j.state = :processingState
        AND j.attemptStartedAt IS NOT NULL
        AND j.attemptStartedAt <= :staleAttemptStartedAtOrBefore
      ORDER BY j.attemptStartedAt ASC, j.id ASC
      """)
  List<ArticleIngestionJob> findStaleProcessingJobsForUpdate(
      @Param("processingState") IngestionState processingState,
      @Param("staleAttemptStartedAtOrBefore") Instant staleAttemptStartedAtOrBefore,
      Limit limit);
}

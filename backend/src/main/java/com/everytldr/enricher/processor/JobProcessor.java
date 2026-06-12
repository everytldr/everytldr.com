package com.everytldr.enricher.processor;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.enricher.completion.CompletionService;
import com.everytldr.enricher.completion.CompletionStatus;
import com.everytldr.enricher.content.ContentResolver;
import com.everytldr.enricher.enrichment.CategorySlugProvider;
import com.everytldr.enricher.enrichment.EnrichmentClient;
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.everytldr.enricher.enrichment.EnrichmentRequest;
import com.everytldr.enricher.enrichment.EnrichmentResult;
import com.everytldr.enricher.processor.ProcessingResult.Status;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("enricher")
@Slf4j
public class JobProcessor {
  private final ArticleIngestionJobClaimService articleIngestionJobClaimService;
  private final ArticleIngestionJobRepository articleIngestionJobRepository;
  private final CategorySlugProvider categorySlugProvider;
  private final CompletionService completionService;
  private final List<ContentResolver> articleContentResolvers;
  private final List<EnrichmentClient> enrichmentClients;
  private final ProcessingProperties properties;
  private final Clock clock;

  public List<ProcessingResult> processNextBatch(int limit) {
    Instant now = Instant.now(clock);
    return articleIngestionJobClaimService.claimNextJobs(now, limit).stream()
        .map(this::processJob)
        .toList();
  }

  public ProcessingResult processJob(ArticleIngestionJob claimedJob) {
    Objects.requireNonNull(claimedJob, "claimedJob must not be null");
    Long jobId = claimedJob.getId();

    Optional<ArticleIngestionJob> reloadedJob =
        articleIngestionJobRepository.findByIdWithArticle(jobId);
    if (reloadedJob.isEmpty()) {
      return new ProcessingResult(jobId, Status.SKIPPED_NOT_FOUND);
    }

    ArticleIngestionJob job = reloadedJob.get();
    if (job.getState() != IngestionState.PROCESSING) {
      return new ProcessingResult(jobId, Status.SKIPPED_NOT_PROCESSING);
    }

    try {
      Article article = job.getArticle();
      ContentResolver contentResolver = selectContentResolver(article);
      EnrichmentClient enrichmentClient = selectEnrichmentClient();

      String content = contentResolver.resolve(article);
      List<String> categorySlugs = categorySlugProvider.getCategorySlugs();

      List<EnrichmentResult> enrichmentResults =
          enrichmentClient.enrich(EnrichmentRequest.from(article, content, categorySlugs));
      CompletionStatus completionStatus =
          completionService.completeWithResult(jobId, enrichmentResults);
      return ProcessingResult.from(jobId, completionStatus);
    } catch (EnrichmentException e) {
      return completeFailure(jobId, job, e);
    } catch (RuntimeException e) {
      EnrichmentException permanentException =
          EnrichmentException.permanent("unexpected enrichment error: " + e.getMessage(), e);
      return completeFailure(jobId, job, permanentException);
    }
  }

  private ContentResolver selectContentResolver(Article article) {
    List<ContentResolver> resolvers =
        articleContentResolvers.stream().filter(resolver -> resolver.supports(article)).toList();

    boolean hasNoResolver = resolvers.isEmpty();
    if (hasNoResolver) {
      throw EnrichmentException.permanent(
          "unsupported source URL for enrichment: %s".formatted(article.getContentUrl()));
    }

    return resolvers.getFirst();
  }

  private EnrichmentClient selectEnrichmentClient() {
    if (!enrichmentClients.isEmpty()) {
      return enrichmentClients.getFirst();
    }
    throw EnrichmentException.permanent("no enrichment client is configured");
  }

  private ProcessingResult completeFailure(
      Long jobId, ArticleIngestionJob job, EnrichmentException exception) {
    boolean canRetry = exception.isRetryable() && job.getAttemptCount() < properties.maxAttempts();
    CompletionStatus completionStatus;
    if (canRetry) {
      completionStatus =
          completionService.scheduleRetry(
              jobId, Instant.now(clock).plus(properties.retryDelay()), exception.getMessage());
    } else {
      boolean maxAttemptsExhausted =
          exception.isRetryable() && job.getAttemptCount() >= properties.maxAttempts();
      String failureMessage =
          maxAttemptsExhausted
              ? "max attempts exhausted: " + exception.getMessage()
              : exception.getMessage();
      completionStatus = completionService.fail(jobId, failureMessage);
    }

    log.warn(
        "Article enrichment failed. jobId={}, retryable={}, attemptCount={}, status={}",
        jobId,
        exception.isRetryable(),
        job.getAttemptCount(),
        completionStatus,
        exception);
    return ProcessingResult.from(jobId, completionStatus);
  }
}

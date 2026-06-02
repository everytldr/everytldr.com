package com.everytldr.enricher.processing;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.enricher.completion.ArticleEnrichmentCompletionService;
import com.everytldr.enricher.completion.ArticleEnrichmentCompletionStatus;
import com.everytldr.enricher.enrichment.ArticleContent;
import com.everytldr.enricher.enrichment.ArticleContentResolver;
import com.everytldr.enricher.enrichment.ArticleEnrichmentCategoryOption;
import com.everytldr.enricher.enrichment.ArticleEnrichmentCategoryOptionProvider;
import com.everytldr.enricher.enrichment.ArticleEnrichmentClient;
import com.everytldr.enricher.enrichment.ArticleEnrichmentException;
import com.everytldr.enricher.enrichment.ArticleEnrichmentRequest;
import com.everytldr.enricher.enrichment.ArticleEnrichmentResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a DB-claimed enrichment job through content resolution, AI enrichment, and
 * completion.
 */
@Service
@RequiredArgsConstructor
@Profile("enricher")
@Slf4j
public class ArticleEnrichmentJobProcessor {
  private final ArticleIngestionJobClaimService articleIngestionJobClaimService;
  private final ArticleIngestionJobRepository articleIngestionJobRepository;
  private final ArticleEnrichmentCategoryOptionProvider categoryOptionProvider;
  private final ArticleEnrichmentCompletionService articleEnrichmentCompletionService;
  private final List<ArticleContentResolver> articleContentResolvers;
  private final List<ArticleEnrichmentClient> articleEnrichmentClients;
  private final EnricherProcessingProperties properties;
  private final Clock clock;

  public List<ArticleEnrichmentProcessingResult> processNextJobs(int limit) {
    Instant now = Instant.now(clock);
    return articleIngestionJobClaimService.claimNextJobs(now, limit).stream()
        .map(this::processClaimedJob)
        .toList();
  }

  public ArticleEnrichmentProcessingResult processClaimedJob(Long jobId) {
    Objects.requireNonNull(jobId, "jobId must not be null");

    return articleIngestionJobRepository
        .findByIdWithArticle(jobId)
        .map(job -> processExistingJob(jobId, job))
        .orElseGet(
            () ->
                new ArticleEnrichmentProcessingResult(
                    jobId, ArticleEnrichmentProcessingStatus.SKIPPED_NOT_FOUND));
  }

  private ArticleEnrichmentProcessingResult processExistingJob(
      Long jobId, ArticleIngestionJob job) {
    if (job.getState() != IngestionState.PROCESSING) {
      return new ArticleEnrichmentProcessingResult(
          jobId, ArticleEnrichmentProcessingStatus.SKIPPED_NOT_PROCESSING);
    }

    try {
      ArticleContent content = resolveContent(job.getArticle());
      ArticleEnrichmentResult enrichmentResult =
          selectClient().enrich(buildEnrichmentRequest(content));
      ArticleEnrichmentCompletionStatus completionStatus =
          articleEnrichmentCompletionService.completeWithResult(jobId, enrichmentResult);
      return new ArticleEnrichmentProcessingResult(jobId, map(completionStatus));
    } catch (ArticleEnrichmentException e) {
      return completeFailure(jobId, job, e);
    } catch (RuntimeException e) {
      // Treat unknown bugs or configuration errors as terminal to avoid an endless retry loop.
      ArticleEnrichmentException enrichmentException =
          ArticleEnrichmentException.permanent("unexpected enrichment error: " + e.getMessage(), e);
      return completeFailure(jobId, job, enrichmentException);
    }
  }

  private ArticleContent resolveContent(Article article) {
    ArticleContentResolver resolver = selectResolver(article);
    return resolver.resolve(article);
  }

  private ArticleEnrichmentRequest buildEnrichmentRequest(ArticleContent content) {
    List<ArticleEnrichmentCategoryOption> categoryOptions =
        categoryOptionProvider.getCategoryOptions();
    return new ArticleEnrichmentRequest(content, categoryOptions);
  }

  private ArticleContentResolver selectResolver(Article article) {
    List<ArticleContentResolver> resolvers =
        articleContentResolvers.stream().filter(resolver -> resolver.supports(article)).toList();

    // Resolver ownership must be exclusive because multiple matches make source handling ambiguous.
    if (resolvers.size() == 1) {
      return resolvers.getFirst();
    }
    if (resolvers.size() > 1) {
      throw ArticleEnrichmentException.permanent(
          "multiple content resolvers support sourceUrl: %s".formatted(article.getSourceUrl()));
    }
    throw ArticleEnrichmentException.permanent(
        "unsupported source URL for enrichment: %s".formatted(article.getSourceUrl()));
  }

  private ArticleEnrichmentClient selectClient() {
    if (articleEnrichmentClients.size() == 1) {
      return articleEnrichmentClients.getFirst();
    }
    if (articleEnrichmentClients.size() > 1) {
      throw ArticleEnrichmentException.permanent("multiple enrichment clients are configured");
    }
    throw ArticleEnrichmentException.permanent("no enrichment client is configured");
  }

  private ArticleEnrichmentProcessingResult completeFailure(
      Long jobId, ArticleIngestionJob job, ArticleEnrichmentException exception) {
    ArticleEnrichmentCompletionStatus completionStatus;
    if (exception.isRetryable() && job.getAttemptCount() < properties.maxAttempts()) {
      completionStatus =
          articleEnrichmentCompletionService.scheduleRetry(
              jobId, Instant.now(clock).plus(properties.retryDelay()), exception.getMessage());
    } else {
      completionStatus =
          articleEnrichmentCompletionService.fail(jobId, buildFailureMessage(job, exception));
    }

    log.warn(
        "Article enrichment failed. jobId={}, retryable={}, attemptCount={}, status={}",
        jobId,
        exception.isRetryable(),
        job.getAttemptCount(),
        completionStatus,
        exception);
    return new ArticleEnrichmentProcessingResult(jobId, map(completionStatus));
  }

  private String buildFailureMessage(
      ArticleIngestionJob job, ArticleEnrichmentException exception) {
    if (exception.isRetryable() && job.getAttemptCount() >= properties.maxAttempts()) {
      return "max attempts exhausted: " + exception.getMessage();
    }
    return exception.getMessage();
  }

  private ArticleEnrichmentProcessingStatus map(ArticleEnrichmentCompletionStatus status) {
    return switch (status) {
      case SUCCEEDED -> ArticleEnrichmentProcessingStatus.SUCCEEDED;
      case FAILED -> ArticleEnrichmentProcessingStatus.FAILED;
      case RETRY_SCHEDULED -> ArticleEnrichmentProcessingStatus.RETRY_SCHEDULED;
      case SKIPPED_NOT_PROCESSING -> ArticleEnrichmentProcessingStatus.SKIPPED_NOT_PROCESSING;
    };
  }
}

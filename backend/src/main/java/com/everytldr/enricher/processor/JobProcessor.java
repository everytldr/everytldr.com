package com.everytldr.enricher.processor;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import com.everytldr.enricher.completion.CompletionService;
import com.everytldr.enricher.completion.CompletionStatus;
import com.everytldr.enricher.content.ContentResolver;
import com.everytldr.enricher.content.ContentResolver.ResolvedArticle;
import com.everytldr.enricher.enrichment.CategorySlugProvider;
import com.everytldr.enricher.enrichment.EnrichmentClient;
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.everytldr.enricher.enrichment.EnrichmentRequest;
import com.everytldr.enricher.enrichment.EnrichmentResult;
import com.everytldr.enricher.processor.ProcessingResult.Status;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
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
  private final LicensePolicyEvaluator licensePolicyEvaluator;
  private final EnricherMetrics enricherMetrics;

  public List<ProcessingResult> processNextBatch(int limit) {
    Instant now = Instant.now(clock);
    return articleIngestionJobClaimService.claimNextJobs(now, limit).stream()
        .map(this::processJob)
        .toList();
  }

  public ProcessingResult processJob(ArticleIngestionJob claimedJob) {
    Objects.requireNonNull(claimedJob, "claimedJob must not be null");
    long startedAtNanos = System.nanoTime();
    Long jobId = claimedJob.getId();
    int claimedAttemptCount = claimedJob.getAttemptCount();

    Optional<ArticleIngestionJob> reloadedJob =
        articleIngestionJobRepository.findByIdWithArticle(jobId);
    if (reloadedJob.isEmpty()) {
      return recordResult(new ProcessingResult(jobId, Status.SKIPPED_NOT_FOUND), startedAtNanos);
    }

    ArticleIngestionJob job = reloadedJob.get();
    if (job.getState() != IngestionState.PROCESSING
        || job.getAttemptCount() != claimedAttemptCount) {
      return recordResult(
          new ProcessingResult(jobId, Status.SKIPPED_NOT_PROCESSING), startedAtNanos);
    }

    try {
      Article article = job.getArticle();
      assertArticleLicenseCanBePublished(article);
      ContentResolver contentResolver = selectContentResolver(article);
      EnrichmentClient enrichmentClient = selectEnrichmentClient();

      ResolvedArticle resolvedArticle =
          measureExternalStage(
              EnricherMetrics.ExternalStage.CONTENT_RESOLUTION,
              () -> contentResolver.resolve(article));
      List<String> categorySlugs = categorySlugProvider.getCategorySlugs();

      List<EnrichmentResult> enrichmentResults =
          measureExternalStage(
              EnricherMetrics.ExternalStage.ENRICHMENT,
              () ->
                  enrichmentClient.enrich(
                      EnrichmentRequest.from(article, resolvedArticle.content(), categorySlugs)));
      CompletionStatus completionStatus =
          completionService.completeWithResult(
              jobId, claimedAttemptCount, resolvedArticle.thumbnailUrl(), enrichmentResults);
      return recordResult(ProcessingResult.from(jobId, completionStatus), startedAtNanos);
    } catch (EnrichmentException e) {
      return recordResult(completeFailure(jobId, claimedAttemptCount, e), startedAtNanos);
    } catch (RuntimeException e) {
      EnrichmentException permanentException =
          EnrichmentException.permanent("unexpected enrichment error: " + e.getMessage(), e);
      return recordResult(
          completeFailure(jobId, claimedAttemptCount, permanentException), startedAtNanos);
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
    if (resolvers.size() > 1) {
      throw EnrichmentException.permanent(
          "multiple content resolvers support source URL: %s".formatted(article.getContentUrl()));
    }

    return resolvers.getFirst();
  }

  private void assertArticleLicenseCanBePublished(Article article) {
    LicenseInfo licenseInfo =
        article.getLicenseInfo() == null ? LicenseInfo.createUnknown() : article.getLicenseInfo();
    if (licensePolicyEvaluator.canPublishTransformedText(licenseInfo)) {
      return;
    }

    throw EnrichmentException.permanent(
        "article license does not allow transformed text publishing: licenseCode=%s"
            .formatted(licenseInfo.getLicenseCode().value()));
  }

  private EnrichmentClient selectEnrichmentClient() {
    if (enrichmentClients.isEmpty()) {
      throw EnrichmentException.permanent("no enrichment client is configured");
    }
    if (enrichmentClients.size() > 1) {
      throw EnrichmentException.permanent(
          "multiple enrichment clients are configured: %d".formatted(enrichmentClients.size()));
    }
    return enrichmentClients.getFirst();
  }

  private ProcessingResult completeFailure(
      Long jobId, int attemptCount, EnrichmentException exception) {
    boolean canRetry = exception.isRetryable() && attemptCount < properties.maxAttempts();
    CompletionStatus completionStatus;
    if (canRetry) {
      completionStatus =
          completionService.scheduleRetry(
              jobId,
              attemptCount,
              Instant.now(clock).plus(properties.calculateRetryDelay(attemptCount)),
              exception.getMessage());
    } else {
      boolean maxAttemptsExhausted =
          exception.isRetryable() && attemptCount >= properties.maxAttempts();
      String failureMessage =
          maxAttemptsExhausted
              ? "max attempts exhausted: " + exception.getMessage()
              : exception.getMessage();
      completionStatus = completionService.fail(jobId, attemptCount, failureMessage);
    }

    log.warn(
        "Article enrichment failed. jobId={}, retryable={}, attemptCount={}, status={}",
        jobId,
        exception.isRetryable(),
        attemptCount,
        completionStatus,
        exception);
    return ProcessingResult.from(jobId, completionStatus);
  }

  private <T> T measureExternalStage(EnricherMetrics.ExternalStage stage, Supplier<T> operation) {
    long startedAtNanos = System.nanoTime();
    try {
      T result = operation.get();
      recordExternalStage(stage, EnricherMetrics.ExternalStageOutcome.SUCCESS, startedAtNanos);
      return result;
    } catch (EnrichmentException e) {
      EnricherMetrics.ExternalStageOutcome outcome =
          e.isRetryable()
              ? EnricherMetrics.ExternalStageOutcome.RETRYABLE_FAILURE
              : EnricherMetrics.ExternalStageOutcome.PERMANENT_FAILURE;
      recordExternalStage(stage, outcome, startedAtNanos);
      throw e;
    } catch (RuntimeException e) {
      recordExternalStage(
          stage, EnricherMetrics.ExternalStageOutcome.PERMANENT_FAILURE, startedAtNanos);
      throw e;
    }
  }

  private void recordExternalStage(
      EnricherMetrics.ExternalStage stage,
      EnricherMetrics.ExternalStageOutcome outcome,
      long startedAtNanos) {
    enricherMetrics.recordExternalStage(
        stage, outcome, Duration.ofNanos(System.nanoTime() - startedAtNanos));
  }

  private ProcessingResult recordResult(ProcessingResult result, long startedAtNanos) {
    enricherMetrics.recordJob(
        result.status(), Duration.ofNanos(System.nanoTime() - startedAtNanos));
    return result;
  }
}

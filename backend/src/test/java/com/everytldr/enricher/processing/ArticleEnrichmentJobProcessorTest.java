package com.everytldr.enricher.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleEnrichmentJobProcessorTest {
  private static final Instant NOW = Instant.parse("2026-05-28T01:02:03Z");
  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final int MAX_ATTEMPTS = 2;
  private static final Duration RETRY_DELAY = Duration.ofMinutes(7);

  @Mock private ArticleIngestionJobClaimService articleIngestionJobClaimService;

  @Mock private ArticleIngestionJobRepository articleIngestionJobRepository;

  @Mock private ArticleEnrichmentCategoryOptionProvider categoryOptionProvider;

  @Mock private ArticleEnrichmentCompletionService articleEnrichmentCompletionService;

  @Mock private ArticleContentResolver articleContentResolver;

  @Mock private ArticleEnrichmentClient articleEnrichmentClient;

  private ArticleEnrichmentJobProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new ArticleEnrichmentJobProcessor(
            articleIngestionJobClaimService,
            articleIngestionJobRepository,
            categoryOptionProvider,
            articleEnrichmentCompletionService,
            List.of(articleContentResolver),
            List.of(articleEnrichmentClient),
            new EnricherProcessingProperties(
                false,
                10,
                Duration.ofSeconds(30),
                MAX_ATTEMPTS,
                RETRY_DELAY,
                Duration.ofMinutes(15)),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void processNextJobsClaimsAndCompletesClaimedJobs() {
    Long jobId = 100L;
    ArticleIngestionJob job = processingJob("https://globalvoices.org/example", 1);
    ArticleContent content = content(job.getArticle());
    ArticleEnrichmentRequest enrichmentRequest =
        new ArticleEnrichmentRequest(content, categoryOptions());
    ArticleEnrichmentResult enrichmentResult = validResult();

    when(articleIngestionJobClaimService.claimNextJobs(NOW, 2)).thenReturn(List.of(jobId));
    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.of(job));
    when(articleContentResolver.supports(job.getArticle())).thenReturn(true);
    when(articleContentResolver.resolve(job.getArticle())).thenReturn(content);
    when(categoryOptionProvider.getCategoryOptions()).thenReturn(categoryOptions());
    when(articleEnrichmentClient.enrich(enrichmentRequest)).thenReturn(enrichmentResult);
    when(articleEnrichmentCompletionService.completeWithResult(jobId, enrichmentResult))
        .thenReturn(ArticleEnrichmentCompletionStatus.SUCCEEDED);

    List<ArticleEnrichmentProcessingResult> results = processor.processNextJobs(2);

    assertThat(results)
        .containsExactly(
            new ArticleEnrichmentProcessingResult(
                jobId, ArticleEnrichmentProcessingStatus.SUCCEEDED));
  }

  @Test
  void retryableFailureSchedulesRetryWithConfiguredDelayWhenAttemptsRemain() {
    Long jobId = 101L;
    ArticleIngestionJob job = processingJob("https://globalvoices.org/retry", 1);

    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.of(job));
    when(articleContentResolver.supports(job.getArticle())).thenReturn(true);
    when(articleContentResolver.resolve(job.getArticle()))
        .thenThrow(ArticleEnrichmentException.retryable("content timeout"));
    when(articleEnrichmentCompletionService.scheduleRetry(
            jobId, NOW.plus(RETRY_DELAY), "content timeout"))
        .thenReturn(ArticleEnrichmentCompletionStatus.RETRY_SCHEDULED);

    ArticleEnrichmentProcessingResult result = processor.processClaimedJob(jobId);

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentProcessingResult(
                jobId, ArticleEnrichmentProcessingStatus.RETRY_SCHEDULED));
    verifyNoInteractions(articleEnrichmentClient);
  }

  @Test
  void retryableFailureFailsWhenMaxAttemptsAreExhausted() {
    Long jobId = 102L;
    ArticleIngestionJob job = processingJob("https://globalvoices.org/max-attempts", MAX_ATTEMPTS);

    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.of(job));
    when(articleContentResolver.supports(job.getArticle())).thenReturn(true);
    when(articleContentResolver.resolve(job.getArticle()))
        .thenThrow(ArticleEnrichmentException.retryable("content timeout"));
    when(articleEnrichmentCompletionService.fail(jobId, "max attempts exhausted: content timeout"))
        .thenReturn(ArticleEnrichmentCompletionStatus.FAILED);

    ArticleEnrichmentProcessingResult result = processor.processClaimedJob(jobId);

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentProcessingResult(jobId, ArticleEnrichmentProcessingStatus.FAILED));
  }

  @Test
  void unsupportedSourceFailsWithoutCallingEnrichmentClient() {
    Long jobId = 103L;
    ArticleIngestionJob job = processingJob("https://unsupported.example.com/article", 1);

    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.of(job));
    when(articleContentResolver.supports(job.getArticle())).thenReturn(false);
    when(articleEnrichmentCompletionService.fail(
            jobId,
            "unsupported source URL for enrichment: https://unsupported.example.com/article"))
        .thenReturn(ArticleEnrichmentCompletionStatus.FAILED);

    ArticleEnrichmentProcessingResult result = processor.processClaimedJob(jobId);

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentProcessingResult(jobId, ArticleEnrichmentProcessingStatus.FAILED));
    verifyNoInteractions(articleEnrichmentClient);
  }

  @Test
  void categoryProviderPermanentFailureFailsWithoutCallingEnrichmentClient() {
    Long jobId = 104L;
    ArticleIngestionJob job = processingJob("https://globalvoices.org/categories", 1);
    ArticleContent content = content(job.getArticle());

    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.of(job));
    when(articleContentResolver.supports(job.getArticle())).thenReturn(true);
    when(articleContentResolver.resolve(job.getArticle())).thenReturn(content);
    when(categoryOptionProvider.getCategoryOptions())
        .thenThrow(ArticleEnrichmentException.permanent("no categories configured for enrichment"));
    when(articleEnrichmentCompletionService.fail(jobId, "no categories configured for enrichment"))
        .thenReturn(ArticleEnrichmentCompletionStatus.FAILED);

    ArticleEnrichmentProcessingResult result = processor.processClaimedJob(jobId);

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentProcessingResult(jobId, ArticleEnrichmentProcessingStatus.FAILED));
    verifyNoInteractions(articleEnrichmentClient);
  }

  @Test
  void nonProcessingJobIsSkipped() {
    Long jobId = 105L;
    ArticleIngestionJob job = pendingJob("https://globalvoices.org/pending");

    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.of(job));

    ArticleEnrichmentProcessingResult result = processor.processClaimedJob(jobId);

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentProcessingResult(
                jobId, ArticleEnrichmentProcessingStatus.SKIPPED_NOT_PROCESSING));
    verifyNoInteractions(
        articleContentResolver, articleEnrichmentClient, articleEnrichmentCompletionService);
  }

  @Test
  void missingJobIsSkipped() {
    Long jobId = 106L;
    when(articleIngestionJobRepository.findByIdWithArticle(jobId)).thenReturn(Optional.empty());

    ArticleEnrichmentProcessingResult result = processor.processClaimedJob(jobId);

    assertThat(result)
        .isEqualTo(
            new ArticleEnrichmentProcessingResult(
                jobId, ArticleEnrichmentProcessingStatus.SKIPPED_NOT_FOUND));
  }

  private ArticleIngestionJob processingJob(String sourceUrl, int attemptCount) {
    ArticleIngestionJob job = pendingJob(sourceUrl);
    for (int i = 0; i < attemptCount; i++) {
      Instant attemptStartedAt = NOW.plusSeconds(i);
      boolean claimed = job.claimForAttempt(attemptStartedAt);
      assertThat(claimed).isTrue();
      if (i < attemptCount - 1) {
        job.scheduleRetry(attemptStartedAt, "temporary failure");
      }
    }
    return job;
  }

  private ArticleIngestionJob pendingJob(String sourceUrl) {
    Article article = Article.create(sourceUrl, "Global Voices", null, "en", PUBLISHED_AT);
    return ArticleIngestionJob.create(article, sha256(sourceUrl));
  }

  private ArticleContent content(Article article) {
    return new ArticleContent(
        article.getSourceUrl(), article.getSource(), article.getLanguage(), "Full article body");
  }

  private ArticleEnrichmentResult validResult() {
    return new ArticleEnrichmentResult(
        "KO title", "KO summary", "EN title", "EN summary", "global-voices-politics");
  }

  private List<ArticleEnrichmentCategoryOption> categoryOptions() {
    return List.of(
        new ArticleEnrichmentCategoryOption("global-voices"),
        new ArticleEnrichmentCategoryOption("global-voices-politics"));
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

package com.everytldr.enricher.processor;

import static com.everytldr.enricher.processor.ProcessingResult.Status.FAILED;
import static com.everytldr.enricher.processor.ProcessingResult.Status.RETRY_SCHEDULED;
import static com.everytldr.enricher.processor.ProcessingResult.Status.SKIPPED_NOT_FOUND;
import static com.everytldr.enricher.processor.ProcessingResult.Status.SKIPPED_NOT_PROCESSING;
import static com.everytldr.enricher.processor.ProcessingResult.Status.SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.enricher.completion.CompletionService;
import com.everytldr.enricher.completion.CompletionStatus;
import com.everytldr.enricher.content.ContentResolver;
import com.everytldr.enricher.content.ContentResolver.ResolvedArticle;
import com.everytldr.enricher.enrichment.CategorySlugProvider;
import com.everytldr.enricher.enrichment.EnrichmentClient;
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.everytldr.enricher.enrichment.EnrichmentRequest;
import com.everytldr.enricher.enrichment.EnrichmentResult;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobProcessorTest {
  private static final Instant NOW = Instant.parse("2026-05-28T01:02:03Z");
  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final Duration RETRY_DELAY = Duration.ofMinutes(7);
  private static final int MAX_ATTEMPTS = 2;
  private static final String ARTICLE_BODY = "Full article body";

  @Mock private ArticleIngestionJobClaimService claimService;
  @Mock private ArticleIngestionJobRepository jobRepository;
  @Mock private CategorySlugProvider categorySlugProvider;
  @Mock private CompletionService completionService;
  @Mock private ContentResolver contentResolver;
  @Mock private EnrichmentClient enrichmentClient;

  private JobProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new JobProcessor(
            claimService,
            jobRepository,
            categorySlugProvider,
            completionService,
            List.of(contentResolver),
            List.of(enrichmentClient),
            new ProcessingProperties(
                false,
                10,
                Duration.ofSeconds(30),
                MAX_ATTEMPTS,
                RETRY_DELAY,
                Duration.ofMinutes(15)),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void processNextBatchEnrichesClaimedJobs() {
    ArticleIngestionJob job = processingJob(100L, "https://globalvoices.org/example", 1);
    List<EnrichmentResult> results = validResults();

    when(claimService.claimNextJobs(NOW, 2)).thenReturn(List.of(job));
    when(jobRepository.findByIdWithArticle(job.getId())).thenReturn(Optional.of(job));
    when(contentResolver.supports(job.getArticle())).thenReturn(true);
    when(contentResolver.resolve(job.getArticle()))
        .thenReturn(new ResolvedArticle(ARTICLE_BODY, null));
    when(categorySlugProvider.getCategorySlugs()).thenReturn(categorySlugs());
    when(enrichmentClient.enrich(enrichmentRequest(job.getArticle()))).thenReturn(results);
    when(completionService.completeWithResult(job.getId(), null, results))
        .thenReturn(CompletionStatus.SUCCEEDED);

    assertThat(processor.processNextBatch(2))
        .containsExactly(new ProcessingResult(job.getId(), SUCCEEDED));
  }

  @Test
  void retryableFailureSchedulesRetryWhenAttemptsRemain() {
    ArticleIngestionJob job = processingJob(101L, "https://globalvoices.org/retry", 1);

    when(jobRepository.findByIdWithArticle(job.getId())).thenReturn(Optional.of(job));
    when(contentResolver.supports(job.getArticle())).thenReturn(true);
    when(contentResolver.resolve(job.getArticle()))
        .thenThrow(EnrichmentException.retryable("content timeout"));
    when(completionService.scheduleRetry(job.getId(), NOW.plus(RETRY_DELAY), "content timeout"))
        .thenReturn(CompletionStatus.RETRY_SCHEDULED);

    assertThat(processor.processJob(job))
        .isEqualTo(new ProcessingResult(job.getId(), RETRY_SCHEDULED));
    verifyNoInteractions(enrichmentClient);
  }

  @Test
  void retryableFailureFailsWhenMaxAttemptsAreExhausted() {
    ArticleIngestionJob job =
        processingJob(102L, "https://globalvoices.org/max-attempts", MAX_ATTEMPTS);

    when(jobRepository.findByIdWithArticle(job.getId())).thenReturn(Optional.of(job));
    when(contentResolver.supports(job.getArticle())).thenReturn(true);
    when(contentResolver.resolve(job.getArticle()))
        .thenThrow(EnrichmentException.retryable("content timeout"));
    when(completionService.fail(job.getId(), "max attempts exhausted: content timeout"))
        .thenReturn(CompletionStatus.FAILED);

    assertThat(processor.processJob(job)).isEqualTo(new ProcessingResult(job.getId(), FAILED));
  }

  @Test
  void unsupportedArticleFailsBeforeEnrichment() {
    ArticleIngestionJob job = processingJob(103L, "https://unsupported.example.com/article", 1);

    when(jobRepository.findByIdWithArticle(job.getId())).thenReturn(Optional.of(job));
    when(contentResolver.supports(job.getArticle())).thenReturn(false);
    when(completionService.fail(
            job.getId(),
            "unsupported source URL for enrichment: https://unsupported.example.com/article"))
        .thenReturn(CompletionStatus.FAILED);

    assertThat(processor.processJob(job)).isEqualTo(new ProcessingResult(job.getId(), FAILED));
    verifyNoInteractions(enrichmentClient);
  }

  @Test
  void skipsJobsThatCannotBeProcessed() {
    ArticleIngestionJob pendingJob = pendingJob(104L, "https://globalvoices.org/pending");
    ArticleIngestionJob missingJob = processingJob(105L, "https://globalvoices.org/missing", 1);

    when(jobRepository.findByIdWithArticle(pendingJob.getId())).thenReturn(Optional.of(pendingJob));
    when(jobRepository.findByIdWithArticle(missingJob.getId())).thenReturn(Optional.empty());

    assertThat(processor.processJob(pendingJob))
        .isEqualTo(new ProcessingResult(pendingJob.getId(), SKIPPED_NOT_PROCESSING));
    assertThat(processor.processJob(missingJob))
        .isEqualTo(new ProcessingResult(missingJob.getId(), SKIPPED_NOT_FOUND));
    verifyNoInteractions(contentResolver, enrichmentClient, completionService);
  }

  @Test
  void permanentUnexpectedErrorsAreMarkedFailed() {
    ArticleIngestionJob job = processingJob(106L, "https://globalvoices.org/unexpected", 1);

    when(jobRepository.findByIdWithArticle(job.getId())).thenReturn(Optional.of(job));
    when(contentResolver.supports(job.getArticle())).thenReturn(true);
    when(contentResolver.resolve(job.getArticle()))
        .thenReturn(new ResolvedArticle(ARTICLE_BODY, null));
    when(categorySlugProvider.getCategorySlugs()).thenReturn(categorySlugs());
    when(enrichmentClient.enrich(enrichmentRequest(job.getArticle())))
        .thenThrow(new IllegalStateException("bad response"));
    when(completionService.fail(job.getId(), "unexpected enrichment error: bad response"))
        .thenReturn(CompletionStatus.FAILED);

    assertThat(processor.processJob(job)).isEqualTo(new ProcessingResult(job.getId(), FAILED));
    verify(completionService).fail(job.getId(), "unexpected enrichment error: bad response");
  }

  private ArticleIngestionJob processingJob(Long id, String sourceUrl, int attemptCount) {
    ArticleIngestionJob job = pendingJob(id, sourceUrl);
    for (int attempt = 0; attempt < attemptCount; attempt++) {
      Instant attemptStartedAt = NOW.plusSeconds(attempt);
      assertThat(job.claimForAttempt(attemptStartedAt)).isTrue();
      if (attempt < attemptCount - 1) {
        job.scheduleRetry(attemptStartedAt, "temporary failure");
      }
    }
    return job;
  }

  private ArticleIngestionJob pendingJob(Long id, String sourceUrl) {
    Article article = Article.create(sourceUrl, "Global Voices", null, "en", PUBLISHED_AT);
    ArticleIngestionJob job = ArticleIngestionJob.create(article, sha256(sourceUrl));
    ReflectionTestUtils.setField(job, "id", id);
    return job;
  }

  private EnrichmentRequest enrichmentRequest(Article article) {
    return new EnrichmentRequest(
        article.getContentUrl(),
        article.getSource(),
        article.getLanguage(),
        ARTICLE_BODY,
        categorySlugs());
  }

  private List<EnrichmentResult> validResults() {
    return List.of(
        new EnrichmentResult("ko", "KO title", "KO summary", "politics"),
        new EnrichmentResult("en", "EN title", "EN summary", "politics"));
  }

  private List<String> categorySlugs() {
    return List.of("media", "politics");
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}

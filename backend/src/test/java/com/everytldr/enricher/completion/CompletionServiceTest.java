package com.everytldr.enricher.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleSummary;
import com.everytldr.common.domain.article.ArticleSummaryRepository;
import com.everytldr.common.domain.category.ArticleCategoryRepository;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.enricher.enrichment.EnrichmentResult;
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
class CompletionServiceTest {

  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");
  private static final String SOURCE_NAME = "Example Source";

  @Autowired private CompletionService completionService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository articleSummaryRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private ArticleIngestionJobRepository jobRepository;
  @Autowired private ArticleSourceRepository sourceRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void completeWithResultSavesSummariesCategoryAndMarksJobSucceeded() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/success");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(),
            job.getAttemptCount(),
            "https://example.com/thumbnail.jpg",
            validResults());
    flushAndClear();

    assertThat(status).isEqualTo(CompletionStatus.SUCCEEDED);
    assertThat(jobRepository.findById(job.getId()).orElseThrow().getState())
        .isEqualTo(IngestionState.SUCCEEDED);
    assertThat(articleRepository.findById(articleId).orElseThrow().getThumbnailUrl())
        .isEqualTo("https://example.com/thumbnail.jpg");
    assertSummary(articleId, SupportedLanguage.KOREAN.code(), "KO title", "KO summary");
    assertSummary(articleId, SupportedLanguage.ENGLISH.code(), "EN title", "EN summary");
    assertThat(articleCategoryRepository.findAllByArticleId(articleId))
        .singleElement()
        .extracting(articleCategory -> articleCategory.getCategory().getSlug())
        .isEqualTo("politics-government-governance");
  }

  @Test
  void completeWithResultKeepsExistingThumbnailUrl() {
    source();
    Article article =
        articleRepository.saveAndFlush(
            Article.create(
                "https://example.com/enricher/existing-thumbnail",
                SOURCE_NAME,
                "https://example.com/original.jpg",
                "en",
                PUBLISHED_AT,
                licenseInfo()));
    ArticleIngestionJob job =
        jobRepository.saveAndFlush(ArticleIngestionJob.create(article, sha256("existing")));
    assertThat(job.claimForAttempt(NOW)).isTrue();
    jobRepository.saveAndFlush(job);
    Long articleId = article.getId();
    flushAndClear();

    completionService.completeWithResult(
        job.getId(), job.getAttemptCount(), "https://example.com/replacement.jpg", validResults());
    flushAndClear();

    assertThat(articleRepository.findById(articleId).orElseThrow().getThumbnailUrl())
        .isEqualTo("https://example.com/original.jpg");
  }

  @Test
  void completeWithResultRewritesExistingSummaries() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/rewrite");
    Article article = job.getArticle();
    ArticleSummary oldKo =
        articleSummaryRepository.saveAndFlush(
            ArticleSummary.create(article, SupportedLanguage.KOREAN.code(), "old ko", "old body"));
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(), job.getAttemptCount(), null, validResults());
    flushAndClear();

    ArticleSummary rewrittenKo =
        articleSummaryRepository
            .findByArticleIdAndLanguage(article.getId(), SupportedLanguage.KOREAN.code())
            .orElseThrow();
    assertThat(status).isEqualTo(CompletionStatus.SUCCEEDED);
    assertThat(rewrittenKo.getId()).isEqualTo(oldKo.getId());
    assertThat(rewrittenKo.getTitle()).isEqualTo("KO title");
    assertThat(rewrittenKo.getContent()).isEqualTo("KO summary");
  }

  @Test
  void invalidResultMarksJobFailedWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/invalid");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(),
            job.getAttemptCount(),
            "https://example.com/thumbnail.jpg",
            List.of(new EnrichmentResult("ko", "", "KO summary", "media")));
    flushAndClear();

    ArticleIngestionJob reloadedJob = jobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(CompletionStatus.FAILED);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getLastErrorMessage())
        .isEqualTo("invalid enrichment result: title is blank");
    assertNoEnrichmentWrites(articleId);
  }

  @Test
  void duplicateLanguageResultMarksJobFailedWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/duplicate-language");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(),
            job.getAttemptCount(),
            "https://example.com/thumbnail.jpg",
            List.of(
                new EnrichmentResult(
                    "ko", "KO title", "KO summary", "politics-government-governance"),
                new EnrichmentResult(
                    "ko",
                    "Second KO title",
                    "Second KO summary",
                    "politics-government-governance")));
    flushAndClear();

    assertThat(status).isEqualTo(CompletionStatus.FAILED);
    assertThat(jobRepository.findById(job.getId()).orElseThrow().getLastErrorMessage())
        .isEqualTo("invalid enrichment result: duplicate language: ko");
    assertNoEnrichmentWrites(articleId);
  }

  @Test
  void missingLanguageResultMarksJobFailedWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/missing-language");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(),
            job.getAttemptCount(),
            "https://example.com/thumbnail.jpg",
            List.of(
                new EnrichmentResult(
                    "ko", "KO title", "KO summary", "politics-government-governance")));
    flushAndClear();

    assertThat(status).isEqualTo(CompletionStatus.FAILED);
    assertThat(jobRepository.findById(job.getId()).orElseThrow().getLastErrorMessage())
        .isEqualTo(
            "invalid enrichment result: languages must include each supported language exactly once");
    assertNoEnrichmentWrites(articleId);
  }

  @Test
  void mismatchedCategoryResultsMarkJobFailedWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/mismatched-category");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(),
            job.getAttemptCount(),
            "https://example.com/thumbnail.jpg",
            List.of(
                new EnrichmentResult(
                    "ko", "KO title", "KO summary", "politics-government-governance"),
                new EnrichmentResult("en", "EN title", "EN summary", "media")));
    flushAndClear();

    assertThat(status).isEqualTo(CompletionStatus.FAILED);
    assertThat(jobRepository.findById(job.getId()).orElseThrow().getLastErrorMessage())
        .isEqualTo("invalid enrichment result: categorySlug values do not match");
    assertNoEnrichmentWrites(articleId);
  }

  @Test
  void nonProcessingJobIsSkipped() {
    ArticleIngestionJob job = savePendingJob("https://example.com/enricher/pending");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(), job.getAttemptCount(), null, validResults());
    flushAndClear();

    assertThat(status).isEqualTo(CompletionStatus.SKIPPED_NOT_PROCESSING);
    assertThat(jobRepository.findById(job.getId()).orElseThrow().getState())
        .isEqualTo(IngestionState.PENDING);
    assertNoEnrichmentWrites(articleId);
  }

  @Test
  void staleAttemptCannotCompleteReclaimedJob() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/stale-success");
    Long articleId = job.getArticle().getId();
    int staleAttemptCount = reclaimStaleAttempt(job);
    flushAndClear();

    CompletionStatus status =
        completionService.completeWithResult(
            job.getId(), staleAttemptCount, "https://example.com/thumbnail.jpg", validResults());
    flushAndClear();

    ArticleIngestionJob reloadedJob = jobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(CompletionStatus.SKIPPED_NOT_PROCESSING);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(reloadedJob.getAttemptCount()).isEqualTo(staleAttemptCount + 1);
    assertThat(articleRepository.findById(articleId).orElseThrow().getThumbnailUrl()).isNull();
    assertNoEnrichmentWrites(articleId);
  }

  @Test
  void staleAttemptCannotScheduleRetryOrFailReclaimedJob() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/stale-failure");
    int staleAttemptCount = reclaimStaleAttempt(job);
    flushAndClear();

    CompletionStatus retryStatus =
        completionService.scheduleRetry(
            job.getId(), staleAttemptCount, NOW.plusSeconds(60), "stale retry");
    CompletionStatus failureStatus =
        completionService.fail(job.getId(), staleAttemptCount, "stale failure");
    flushAndClear();

    ArticleIngestionJob reloadedJob = jobRepository.findById(job.getId()).orElseThrow();
    assertThat(retryStatus).isEqualTo(CompletionStatus.SKIPPED_NOT_PROCESSING);
    assertThat(failureStatus).isEqualTo(CompletionStatus.SKIPPED_NOT_PROCESSING);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.PROCESSING);
    assertThat(reloadedJob.getAttemptCount()).isEqualTo(staleAttemptCount + 1);
    assertThat(reloadedJob.getAttemptStartedAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
  }

  private void assertSummary(Long articleId, String language, String title, String content) {
    ArticleSummary summary =
        articleSummaryRepository.findByArticleIdAndLanguage(articleId, language).orElseThrow();

    assertThat(summary.getTitle()).isEqualTo(title);
    assertThat(summary.getContent()).isEqualTo(content);
  }

  private void assertNoEnrichmentWrites(Long articleId) {
    assertThat(articleRepository.findById(articleId).orElseThrow().getThumbnailUrl()).isNull();
    assertThat(
            articleSummaryRepository.findByArticleIdAndLanguage(
                articleId, SupportedLanguage.KOREAN.code()))
        .isEmpty();
    assertThat(
            articleSummaryRepository.findByArticleIdAndLanguage(
                articleId, SupportedLanguage.ENGLISH.code()))
        .isEmpty();
    assertThat(articleCategoryRepository.findAllByArticleId(articleId)).isEmpty();
  }

  private List<EnrichmentResult> validResults() {
    return List.of(
        new EnrichmentResult("ko", "KO title", "KO summary", "politics-government-governance"),
        new EnrichmentResult("en", "EN title", "EN summary", "politics-government-governance"));
  }

  private ArticleIngestionJob saveProcessingJob(String sourceUrl) {
    ArticleIngestionJob job = savePendingJob(sourceUrl);
    assertThat(job.claimForAttempt(NOW)).isTrue();
    return jobRepository.saveAndFlush(job);
  }

  private int reclaimStaleAttempt(ArticleIngestionJob job) {
    int staleAttemptCount = job.getAttemptCount();
    assertThat(
            job.reclaimStaleProcessingAttempt(
                NOW.plus(Duration.ofMinutes(15)), Duration.ofMinutes(15)))
        .isTrue();
    jobRepository.saveAndFlush(job);
    return staleAttemptCount;
  }

  private ArticleIngestionJob savePendingJob(String sourceUrl) {
    source();
    Article article =
        articleRepository.saveAndFlush(
            Article.create(sourceUrl, SOURCE_NAME, null, "en", PUBLISHED_AT, licenseInfo()));
    return jobRepository.saveAndFlush(ArticleIngestionJob.create(article, sha256(sourceUrl)));
  }

  private ArticleSource source() {
    return sourceRepository
        .findByName(SOURCE_NAME)
        .orElseGet(
            () ->
                sourceRepository.saveAndFlush(
                    ArticleSource.create(
                        SOURCE_NAME,
                        new SourcePolicy(
                            new CrawlingPolicy(
                                List.of("https://example.com/feed.xml"),
                                List.of("example.com"),
                                List.of("article"),
                                List.of(),
                                List.of())),
                        "en",
                        SourceType.RSS,
                        licenseInfo())));
  }

  private LicenseInfo licenseInfo() {
    return LicenseInfo.createCcBy("4.0");
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}

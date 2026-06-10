package com.everytldr.enricher.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleSummary;
import com.everytldr.common.domain.article.ArticleSummaryRepository;
import com.everytldr.common.domain.category.ArticleCategory;
import com.everytldr.common.domain.category.ArticleCategoryRepository;
import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.enricher.enrichment.ArticleEnrichmentResult;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
class ArticleEnrichmentCompletionServiceTest {

  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-04T10:15:30Z");
  private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");

  @Autowired private ArticleEnrichmentCompletionService articleEnrichmentCompletionService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleSummaryRepository articleSummaryRepository;

  @Autowired private ArticleCategoryRepository articleCategoryRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private ArticleIngestionJobRepository articleIngestionJobRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void completeWithResultSavesSummariesCategoryAndMarksSucceeded() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/complete-success");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), validResult());
    flushAndClear();

    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.SUCCEEDED);
    assertThat(articleIngestionJobRepository.findById(job.getId()).orElseThrow().getState())
        .isEqualTo(IngestionState.SUCCEEDED);
    assertSummary(articleId, SupportedLanguage.KOREAN.code(), "KO title", "KO summary");
    assertSummary(articleId, SupportedLanguage.ENGLISH.code(), "EN title", "EN summary");
    assertThat(articleCategoryRepository.findAllByArticleId(articleId))
        .singleElement()
        .extracting(articleCategory -> articleCategory.getCategory().getSlug())
        .isEqualTo("global-voices-politics");
  }

  @Test
  void completeWithResultRewritesExistingSummaries() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/rewrite");
    Article article = job.getArticle();
    ArticleSummary oldKo =
        articleSummaryRepository.saveAndFlush(
            ArticleSummary.create(article, SupportedLanguage.KOREAN.code(), "old ko", "old body"));
    ArticleSummary oldEn =
        articleSummaryRepository.saveAndFlush(
            ArticleSummary.create(article, SupportedLanguage.ENGLISH.code(), "old en", "old body"));
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), validResult());
    flushAndClear();

    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.SUCCEEDED);
    ArticleSummary rewrittenKo =
        articleSummaryRepository
            .findByArticleIdAndLanguage(article.getId(), SupportedLanguage.KOREAN.code())
            .orElseThrow();
    ArticleSummary rewrittenEn =
        articleSummaryRepository
            .findByArticleIdAndLanguage(article.getId(), SupportedLanguage.ENGLISH.code())
            .orElseThrow();
    assertThat(rewrittenKo.getId()).isEqualTo(oldKo.getId());
    assertThat(rewrittenKo.getTitle()).isEqualTo("KO title");
    assertThat(rewrittenKo.getContent()).isEqualTo("KO summary");
    assertThat(rewrittenEn.getId()).isEqualTo(oldEn.getId());
    assertThat(rewrittenEn.getTitle()).isEqualTo("EN title");
    assertThat(rewrittenEn.getContent()).isEqualTo("EN summary");
  }

  @Test
  void completeWithResultDoesNotDuplicateExistingCategory() {
    Category politics = politicsCategory();
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/existing-category");
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(job.getArticle(), politics));
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), validResult());
    flushAndClear();

    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.SUCCEEDED);
    assertThat(articleCategoryRepository.findAllByArticleId(articleId)).hasSize(1);
  }

  @Test
  void unknownCategoryFailsWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/unknown-category");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(
            job.getId(),
            new ArticleEnrichmentResult(
                "KO title", "KO summary", "EN title", "EN summary", "unknown-category"));
    flushAndClear();

    ArticleIngestionJob reloadedJob =
        articleIngestionJobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.FAILED);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getLastErrorMessage())
        .isEqualTo("unknown category slug: unknown-category");
    assertNoSummariesOrCategories(articleId);
  }

  @Test
  void invalidResultFailsWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/invalid-result");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(
            job.getId(),
            new ArticleEnrichmentResult(
                "", "KO summary", "EN title", "EN summary", "global-voices-politics"));
    flushAndClear();

    ArticleIngestionJob reloadedJob =
        articleIngestionJobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.FAILED);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getLastErrorMessage())
        .isEqualTo("invalid enrichment result: koTitle is blank");
    assertNoSummariesOrCategories(articleId);
  }

  @Test
  void nullResultFailsWithoutPartialWrites() {
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/null-result");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), null);
    flushAndClear();

    ArticleIngestionJob reloadedJob =
        articleIngestionJobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.FAILED);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getLastErrorMessage())
        .isEqualTo("invalid enrichment result: result is null");
    assertNoSummariesOrCategories(articleId);
  }

  @Test
  void nonProcessingJobIsSkippedWithoutChangingState() {
    ArticleIngestionJob job = savePendingJob("https://example.com/enricher/skipped-pending");
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), validResult());
    flushAndClear();

    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.SKIPPED_NOT_PROCESSING);
    assertThat(articleIngestionJobRepository.findById(job.getId()).orElseThrow().getState())
        .isEqualTo(IngestionState.PENDING);
    assertNoSummariesOrCategories(articleId);
  }

  @Test
  void multipleExistingCategoriesFailWithoutPartialWrites() {
    Category politics = politicsCategory();
    Category otherCategory =
        categoryRepository.saveAndFlush(Category.create("enricher-test-extra", 2));
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/multiple-categories");
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(job.getArticle(), politics));
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(job.getArticle(), otherCategory));
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), validResult());
    flushAndClear();

    ArticleIngestionJob reloadedJob =
        articleIngestionJobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.FAILED);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getLastErrorMessage()).isEqualTo("article has multiple categories");
    assertThat(articleCategoryRepository.findAllByArticleId(articleId)).hasSize(2);
    assertNoSummaries(articleId);
  }

  @Test
  void differentExistingCategoryFailsWithoutPartialWrites() {
    Category otherCategory =
        categoryRepository.saveAndFlush(Category.create("enricher-test-other", 1));
    ArticleIngestionJob job = saveProcessingJob("https://example.com/enricher/category-conflict");
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(job.getArticle(), otherCategory));
    Long articleId = job.getArticle().getId();
    flushAndClear();

    ArticleEnrichmentCompletionStatus status =
        articleEnrichmentCompletionService.completeWithResult(job.getId(), validResult());
    flushAndClear();

    ArticleIngestionJob reloadedJob =
        articleIngestionJobRepository.findById(job.getId()).orElseThrow();
    assertThat(status).isEqualTo(ArticleEnrichmentCompletionStatus.FAILED);
    assertThat(reloadedJob.getState()).isEqualTo(IngestionState.FAILED);
    assertThat(reloadedJob.getLastErrorMessage())
        .isEqualTo("article already has different category");
    assertThat(articleCategoryRepository.findAllByArticleId(articleId))
        .singleElement()
        .extracting(articleCategory -> articleCategory.getCategory().getSlug())
        .isEqualTo("enricher-test-other");
    assertNoSummaries(articleId);
  }

  private void assertSummary(Long articleId, String language, String title, String content) {
    ArticleSummary summary =
        articleSummaryRepository.findByArticleIdAndLanguage(articleId, language).orElseThrow();

    assertThat(summary.getTitle()).isEqualTo(title);
    assertThat(summary.getContent()).isEqualTo(content);
  }

  private void assertNoSummariesOrCategories(Long articleId) {
    assertNoSummaries(articleId);
    assertThat(articleCategoryRepository.findAllByArticleId(articleId)).isEmpty();
  }

  private void assertNoSummaries(Long articleId) {
    assertThat(
            articleSummaryRepository.findByArticleIdAndLanguage(
                articleId, SupportedLanguage.KOREAN.code()))
        .isEmpty();
    assertThat(
            articleSummaryRepository.findByArticleIdAndLanguage(
                articleId, SupportedLanguage.ENGLISH.code()))
        .isEmpty();
  }

  private ArticleEnrichmentResult validResult() {
    return new ArticleEnrichmentResult(
        "KO title", "KO summary", "EN title", "EN summary", "global-voices-politics");
  }

  private Category politicsCategory() {
    return categoryRepository.findBySlug("global-voices-politics").orElseThrow();
  }

  private ArticleIngestionJob saveProcessingJob(String sourceUrl) {
    ArticleIngestionJob job = savePendingJob(sourceUrl);
    boolean claimed = job.claimForAttempt(NOW);
    assertThat(claimed).isTrue();
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private ArticleIngestionJob savePendingJob(String sourceUrl) {
    Article article =
        articleRepository.saveAndFlush(
            Article.create(sourceUrl, "Example Source", null, "en", PUBLISHED_AT));
    ArticleIngestionJob job = ArticleIngestionJob.create(article, sha256(sourceUrl));
    return articleIngestionJobRepository.saveAndFlush(job);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
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

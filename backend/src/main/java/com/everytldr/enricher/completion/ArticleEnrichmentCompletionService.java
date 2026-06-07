package com.everytldr.enricher.completion;

import com.everytldr.common.domain.article.Article;
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
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class ArticleEnrichmentCompletionService {

  private final ArticleIngestionJobRepository articleIngestionJobRepository;
  private final ArticleSummaryRepository articleSummaryRepository;
  private final ArticleCategoryRepository articleCategoryRepository;
  private final CategoryRepository categoryRepository;

  @Transactional
  public ArticleEnrichmentCompletionStatus completeWithResult(
      Long jobId, ArticleEnrichmentResult result) {
    Objects.requireNonNull(jobId, "jobId must not be null");

    ArticleIngestionJob job = findJob(jobId);

    if (job.getState() != IngestionState.PROCESSING) {
      return ArticleEnrichmentCompletionStatus.SKIPPED_NOT_PROCESSING;
    }

    Optional<String> validationErrorMessage = validate(result);
    if (validationErrorMessage.isPresent()) {
      job.markFailed("invalid enrichment result: " + validationErrorMessage.get());
      return ArticleEnrichmentCompletionStatus.FAILED;
    }

    Optional<Category> category = categoryRepository.findBySlug(result.categorySlug());
    if (category.isEmpty()) {
      job.markFailed("unknown category slug: %s".formatted(result.categorySlug()));
      return ArticleEnrichmentCompletionStatus.FAILED;
    }

    Article article = job.getArticle();
    List<ArticleCategory> existingCategories =
        articleCategoryRepository.findAllByArticleId(article.getId());
    Optional<String> categoryConflict =
        validateExistingCategory(existingCategories, category.get());
    if (categoryConflict.isPresent()) {
      job.markFailed(categoryConflict.get());
      return ArticleEnrichmentCompletionStatus.FAILED;
    }

    saveSummary(article, SupportedLanguage.KOREAN.code(), result.koTitle(), result.koSummary());
    saveSummary(article, SupportedLanguage.ENGLISH.code(), result.enTitle(), result.enSummary());

    if (existingCategories.isEmpty()) {
      articleCategoryRepository.save(ArticleCategory.create(article, category.get()));
    }

    job.markSucceeded();
    return ArticleEnrichmentCompletionStatus.SUCCEEDED;
  }

  @Transactional
  public ArticleEnrichmentCompletionStatus scheduleRetry(
      Long jobId, Instant nextAttemptAt, String errorMessage) {
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");

    ArticleIngestionJob job = findJob(jobId);
    if (job.getState() != IngestionState.PROCESSING) {
      return ArticleEnrichmentCompletionStatus.SKIPPED_NOT_PROCESSING;
    }

    job.scheduleRetry(nextAttemptAt, errorMessage);
    return ArticleEnrichmentCompletionStatus.RETRY_SCHEDULED;
  }

  @Transactional
  public ArticleEnrichmentCompletionStatus fail(Long jobId, String errorMessage) {
    Objects.requireNonNull(jobId, "jobId must not be null");

    ArticleIngestionJob job = findJob(jobId);
    if (job.getState() != IngestionState.PROCESSING) {
      return ArticleEnrichmentCompletionStatus.SKIPPED_NOT_PROCESSING;
    }

    job.markFailed(errorMessage);
    return ArticleEnrichmentCompletionStatus.FAILED;
  }

  private Optional<String> validate(ArticleEnrichmentResult result) {
    if (result == null) {
      return Optional.of("result is null");
    }
    return result.validationErrorMessage();
  }

  private ArticleIngestionJob findJob(Long jobId) {
    return articleIngestionJobRepository
        .findById(jobId)
        .orElseThrow(() -> new NoSuchElementException("Article ingestion job not found: " + jobId));
  }

  private Optional<String> validateExistingCategory(
      List<ArticleCategory> existingCategories, Category selectedCategory) {
    if (existingCategories.size() > 1) {
      return Optional.of("article has multiple categories");
    }
    if (existingCategories.isEmpty()) {
      return Optional.empty();
    }

    Category existingCategory = existingCategories.getFirst().getCategory();
    boolean hasConflictingCategory = !existingCategory.getId().equals(selectedCategory.getId());
    if (hasConflictingCategory) {
      return Optional.of("article already has different category");
    }
    return Optional.empty();
  }

  private void saveSummary(Article article, String language, String title, String content) {
    articleSummaryRepository
        .findByArticleIdAndLanguage(article.getId(), language)
        .ifPresentOrElse(
            summary -> summary.rewrite(title, content),
            () ->
                articleSummaryRepository.save(
                    ArticleSummary.create(article, language, title, content)));
  }
}

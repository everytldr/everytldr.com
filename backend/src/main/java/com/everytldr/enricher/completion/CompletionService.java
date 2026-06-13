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
import com.everytldr.enricher.enrichment.EnrichmentException;
import com.everytldr.enricher.enrichment.EnrichmentResult;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class CompletionService {

  private final ArticleIngestionJobRepository articleIngestionJobRepository;
  private final ArticleSummaryRepository articleSummaryRepository;
  private final ArticleCategoryRepository articleCategoryRepository;
  private final CategoryRepository categoryRepository;

  @Transactional
  public CompletionStatus completeWithResult(
      Long jobId, String thumbnailUrl, List<EnrichmentResult> results) {
    Objects.requireNonNull(jobId, "jobId must not be null");

    ArticleIngestionJob job = findJob(jobId);

    if (job.getState() != IngestionState.PROCESSING) {
      return CompletionStatus.SKIPPED_NOT_PROCESSING;
    }

    Article article = job.getArticle();
    Category category;
    List<ArticleCategory> existingCategories;
    try {
      validateResults(results);
      category = findCategory(results.getFirst().categorySlug());
      existingCategories = articleCategoryRepository.findAllByArticleId(article.getId());
      assertExistingCategoryCanBeApplied(existingCategories, category);
    } catch (CompletionFailure | EnrichmentException e) {
      job.markFailed(e.getMessage());
      return CompletionStatus.FAILED;
    }

    boolean canUpdateThumbnailUrl =
        isHttpUrl(thumbnailUrl) && !isHttpUrl(article.getThumbnailUrl());
    if (canUpdateThumbnailUrl) {
      article.updateThumbnailUrl(thumbnailUrl);
    }

    for (EnrichmentResult result : results) {
      saveSummary(article, result.language(), result.title(), result.summary());
    }

    if (existingCategories.isEmpty()) {
      articleCategoryRepository.save(ArticleCategory.create(article, category));
    }

    job.markSucceeded();
    return CompletionStatus.SUCCEEDED;
  }

  private void validateResults(List<EnrichmentResult> results) {
    if (results == null) {
      throw new CompletionFailure("invalid enrichment result: result is null");
    }
    if (results.isEmpty()) {
      throw new CompletionFailure("invalid enrichment result: results must not be empty");
    }
    for (EnrichmentResult result : results) {
      if (result == null) {
        throw new CompletionFailure("invalid enrichment result: result is null");
      }
      result.assertValid();
    }
  }

  @Transactional
  public CompletionStatus scheduleRetry(Long jobId, Instant nextAttemptAt, String errorMessage) {
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");

    ArticleIngestionJob job = findJob(jobId);
    if (job.getState() != IngestionState.PROCESSING) {
      return CompletionStatus.SKIPPED_NOT_PROCESSING;
    }

    job.scheduleRetry(nextAttemptAt, errorMessage);
    return CompletionStatus.RETRY_SCHEDULED;
  }

  @Transactional
  public CompletionStatus fail(Long jobId, String errorMessage) {
    Objects.requireNonNull(jobId, "jobId must not be null");

    ArticleIngestionJob job = findJob(jobId);
    if (job.getState() != IngestionState.PROCESSING) {
      return CompletionStatus.SKIPPED_NOT_PROCESSING;
    }

    job.markFailed(errorMessage);
    return CompletionStatus.FAILED;
  }

  private ArticleIngestionJob findJob(Long jobId) {
    return articleIngestionJobRepository
        .findById(jobId)
        .orElseThrow(() -> new NoSuchElementException("Article ingestion job not found: " + jobId));
  }

  private Category findCategory(String categorySlug) {
    return categoryRepository
        .findBySlug(categorySlug)
        .orElseThrow(
            () -> new CompletionFailure("unknown category slug: %s".formatted(categorySlug)));
  }

  private void assertExistingCategoryCanBeApplied(
      List<ArticleCategory> existingCategories, Category selectedCategory) {
    if (existingCategories.size() > 1) {
      throw new CompletionFailure("article has multiple categories");
    }
    if (existingCategories.isEmpty()) {
      return;
    }

    Category existingCategory = existingCategories.getFirst().getCategory();
    boolean hasConflictingCategory = !existingCategory.getId().equals(selectedCategory.getId());
    if (hasConflictingCategory) {
      throw new CompletionFailure("article already has different category");
    }
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

  private static boolean isHttpUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    try {
      String scheme = URI.create(url).getScheme();
      return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static final class CompletionFailure extends RuntimeException {
    private CompletionFailure(String message) {
      super(message);
    }
  }
}

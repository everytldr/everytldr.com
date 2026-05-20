package org.tldrtimes.enricher.enrichment;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.common.domain.article.Article;
import org.tldrtimes.common.domain.article.ArticleSummary;
import org.tldrtimes.common.domain.article.ArticleSummaryRepository;
import org.tldrtimes.common.domain.category.ArticleCategory;
import org.tldrtimes.common.domain.category.ArticleCategoryRepository;
import org.tldrtimes.common.domain.category.Category;
import org.tldrtimes.common.domain.category.CategoryRepository;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJob;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJobRepository;
import org.tldrtimes.common.domain.ingestion.IngestionState;
import org.tldrtimes.common.domain.language.SupportedLanguage;

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

    ArticleIngestionJob job =
        articleIngestionJobRepository
            .findById(jobId)
            .orElseThrow(
                () -> new NoSuchElementException("Article ingestion job not found: " + jobId));

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

  private Optional<String> validate(ArticleEnrichmentResult result) {
    if (result == null) {
      return Optional.of("result is null");
    }
    return result.validationErrorMessage();
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
    if (!existingCategory.getId().equals(selectedCategory.getId())) {
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

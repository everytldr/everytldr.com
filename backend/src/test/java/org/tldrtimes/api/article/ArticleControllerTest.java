package org.tldrtimes.api.article;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.TestcontainersConfig;
import org.tldrtimes.common.domain.article.Article;
import org.tldrtimes.common.domain.article.ArticleListProjection;
import org.tldrtimes.common.domain.article.ArticleRepository;
import org.tldrtimes.common.domain.article.ArticleSummary;
import org.tldrtimes.common.domain.article.ArticleSummaryRepository;
import org.tldrtimes.common.domain.category.ArticleCategory;
import org.tldrtimes.common.domain.category.ArticleCategoryRepository;
import org.tldrtimes.common.domain.category.Category;
import org.tldrtimes.common.domain.category.CategoryRepository;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class ArticleControllerTest {
  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private ArticleRepository articleRepository;
  @Autowired
  private ArticleSummaryRepository summaryRepository;
  @Autowired
  private ArticleCategoryRepository articleCategoryRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  private Category football;
  private Category tech;

  @BeforeEach
  void seedCategories() {
    football = categoryRepository.saveAndFlush(Category.create("football", 0));
    tech = categoryRepository.saveAndFlush(Category.create("tech", 1));
  }

  @Test
  void ordersByPublishedAtThenIdDescending() {
    Instant when = Instant.parse("2026-04-01T00:00:00Z");
    Article older = saveArticle(when.minus(1, ChronoUnit.HOURS), football, "ko", "older", "본문");
    Article tieA = saveArticle(when, football, "ko", "tieA", "본문");
    Article tieB = saveArticle(when, football, "ko", "tieB", "본문");
    flushAndClear();

    List<ArticleListProjection> rows = findRecent("ko", null, null, null, 10);

    assertThat(rows)
        .extracting(ArticleListProjection::id)
        .containsExactly(tieB.getId(), tieA.getId(), older.getId());
  }

  @Test
  void filtersByLanguageAndCategory() {
    Instant base = Instant.parse("2026-04-01T00:00:00Z");
    Article koFootball = saveArticleWithoutSummary(base, football);
    summaryRepository.saveAndFlush(ArticleSummary.create(koFootball, "ko", "한국어 축구", "본문"));
    summaryRepository.saveAndFlush(ArticleSummary.create(koFootball, "en", "EN football", "body"));
    saveArticle(base.minus(1, ChronoUnit.HOURS), tech, "ko", "한국어 기술", "본문");
    flushAndClear();

    assertThat(findRecent("ko", "football", null, null, 10))
        .singleElement()
        .extracting(ArticleListProjection::title)
        .isEqualTo("한국어 축구");
    assertThat(findRecent("en", null, null, null, 10))
        .singleElement()
        .extracting(ArticleListProjection::title)
        .isEqualTo("EN football");
  }

  @Test
  void excludesArticlesMissingJoinsOrSoftDeleted() {
    Instant base = Instant.parse("2026-04-01T00:00:00Z");
    Article noSummaryInKo = saveArticleWithoutSummary(base, football);
    summaryRepository.saveAndFlush(ArticleSummary.create(noSummaryInKo, "en", "EN", "body"));

    Article noCategory = saveArticleWithoutSummary(base.minus(1, ChronoUnit.HOURS), null);
    summaryRepository.saveAndFlush(ArticleSummary.create(noCategory, "ko", "고아", "본문"));

    Article deleted = saveArticle(base.minus(2, ChronoUnit.HOURS), football, "ko", "삭제", "본문");
    deleted.softDelete(Instant.now());
    articleRepository.saveAndFlush(deleted);

    Article kept = saveArticle(base.minus(3, ChronoUnit.HOURS), football, "ko", "유지", "본문");
    flushAndClear();

    assertThat(findRecent("ko", null, null, null, 10))
        .extracting(ArticleListProjection::id)
        .containsExactly(kept.getId());
  }

  @Test
  void cursorPagesThroughResults() {
    Instant base = Instant.parse("2026-04-01T00:00:00Z");
    for (int i = 0; i < 5; i++) {
      saveArticle(base.minus(i, ChronoUnit.HOURS), football, "ko", "T" + i, "본문");
    }
    flushAndClear();

    List<ArticleListProjection> firstPage = findRecent("ko", null, null, null, 2);
    assertThat(firstPage).extracting(ArticleListProjection::title).containsExactly("T0", "T1");

    ArticleListProjection boundary = firstPage.getLast();
    List<ArticleListProjection> secondPage = findRecent("ko", null, boundary.publishedAt(), boundary.id(), 2);
    assertThat(secondPage).extracting(ArticleListProjection::title).containsExactly("T2", "T3");
  }

  @Test
  void cursorTokenRoundTrips() {
    Instant when = Instant.parse("2026-04-01T12:34:56.789Z");
    long id = 9_876_543_210L;
    String token = ArticleListCursor.encode(when, id);
    ArticleListCursor.Decoded decoded = ArticleListCursor.decode(token);
    assertThat(decoded.publishedAt()).isEqualTo(when);
    assertThat(decoded.id()).isEqualTo(id);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private List<ArticleListProjection> findRecent(
      String language, String slug, Instant cursorAt, Long cursorId, int size) {
    return articleRepository.findRecent(
        language, slug, cursorAt, cursorId, PageRequest.of(0, size, Sort.unsorted()));
  }

  private Article saveArticle(
      Instant publishedAt, Category category, String language, String title, String content) {
    Article article = saveArticleWithoutSummary(publishedAt, category);
    summaryRepository.saveAndFlush(ArticleSummary.create(article, language, title, content));
    return article;
  }

  private Article saveArticleWithoutSummary(Instant publishedAt, Category category) {
    Article article = articleRepository.saveAndFlush(
        Article.create(
            "https://example.com/" + System.nanoTime(), "Example", null, "en", publishedAt));
    if (category != null) {
      articleCategoryRepository.saveAndFlush(ArticleCategory.create(article, category));
    }
    return article;
  }
}

package org.everytldr.api.article;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.everytldr.TestcontainersConfig;
import org.everytldr.common.domain.article.Article;
import org.everytldr.common.domain.article.ArticleComment;
import org.everytldr.common.domain.article.ArticleCommentRepository;
import org.everytldr.common.domain.article.ArticleLike;
import org.everytldr.common.domain.article.ArticleLikeRepository;
import org.everytldr.common.domain.article.ArticleRepository;
import org.everytldr.common.domain.article.ArticleSummary;
import org.everytldr.common.domain.article.ArticleSummaryRepository;
import org.everytldr.common.domain.category.ArticleCategory;
import org.everytldr.common.domain.category.ArticleCategoryRepository;
import org.everytldr.common.domain.category.Category;
import org.everytldr.common.domain.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles({"api", "test"})
@Transactional
class ArticleControllerTest {
  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleLikeRepository likeRepository;
  @Autowired private ArticleCommentRepository commentRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;

  private Category football;

  @BeforeEach
  void seedCategories() {
    football = categoryRepository.saveAndFlush(Category.create("football", 0));
  }

  @Test
  void listReturnsArticlesWithNextCursor() throws Exception {
    Instant base = Instant.parse("2026-04-01T00:00:00Z");
    saveArticle(base, football, "ko", "T0", "본문");
    saveArticle(base.minus(1, ChronoUnit.HOURS), football, "ko", "T1", "본문");
    saveArticle(base.minus(2, ChronoUnit.HOURS), football, "ko", "T2", "본문");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/articles")
                .header("Accept-Language", "ko")
                .param("categoryPrefix", "football")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("T0"))
        .andExpect(jsonPath("$.items[1].title").value("T1"))
        .andExpect(jsonPath("$.nextCursor").isString());
  }

  @Test
  void detailReturnsPublicArticleDataAndAggregateCounts() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    likeRepository.saveAndFlush(ArticleLike.create(article, "a".repeat(64)));
    ArticleLike inactive = likeRepository.saveAndFlush(ArticleLike.create(article, "b".repeat(64)));
    inactive.deactivate();
    commentRepository.saveAndFlush(
        ArticleComment.createTopLevel(
            article, "reader", "p".repeat(60), "c".repeat(64), "203.0", "댓글"));
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/articles/{id}", article.getId()).header("Accept-Language", "ko"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(article.getId().toString()))
        .andExpect(jsonPath("$.title").value("제목"))
        .andExpect(jsonPath("$.summary").value("요약"))
        .andExpect(jsonPath("$.sourceUrl").value(article.getSourceUrl()))
        .andExpect(jsonPath("$.category").value("football"))
        .andExpect(jsonPath("$.likeCount").value(1))
        .andExpect(jsonPath("$.commentCount").value(1))
        .andExpect(jsonPath("$.likedByReader").doesNotExist());
  }

  @Test
  void detailReturnsNotFoundWhenArticleDoesNotExist() throws Exception {
    mockMvc
        .perform(get("/api/articles/{id}", 9_999_999L).header("Accept-Language", "ko"))
        .andExpect(status().isNotFound());
  }

  private Article saveArticle(
      Instant publishedAt, Category category, String language, String title, String content) {
    Article article =
        articleRepository.saveAndFlush(
            Article.create(
                "https://example.com/" + System.nanoTime(), "Example", null, "en", publishedAt));
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(article, category));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, language, title, content));
    return article;
  }
}

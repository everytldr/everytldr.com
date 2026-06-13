package com.everytldr.api.article;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleSummary;
import com.everytldr.common.domain.article.ArticleSummaryRepository;
import com.everytldr.common.domain.category.ArticleCategory;
import com.everytldr.common.domain.category.ArticleCategoryRepository;
import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
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
class ArticleLikeControllerTest {
  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ArticleSourceRepository sourceRepository;

  private Category football;

  @BeforeEach
  void seedFixtures() {
    sourceRepository.saveAndFlush(source());
    football = categoryRepository.saveAndFlush(Category.create("football", 0));
  }

  @Test
  void likeStateIsScopedToCurrentReaderIp() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            put("/api/articles/{id}/likes/me", article.getId())
                .header("X-Forwarded-For", "1.1.1.1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.likedByReader").value(true))
        .andExpect(jsonPath("$.likeCount").value(1));

    mockMvc
        .perform(
            get("/api/articles/{id}/likes/me", article.getId())
                .header("X-Forwarded-For", "1.1.1.1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.likedByReader").value(true))
        .andExpect(jsonPath("$.likeCount").value(1));

    mockMvc
        .perform(
            get("/api/articles/{id}/likes/me", article.getId())
                .header("X-Forwarded-For", "2.2.2.2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.likedByReader").value(false))
        .andExpect(jsonPath("$.likeCount").value(1));

    mockMvc
        .perform(
            delete("/api/articles/{id}/likes/me", article.getId())
                .header("X-Forwarded-For", "1.1.1.1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.likedByReader").value(false))
        .andExpect(jsonPath("$.likeCount").value(0));
  }

  @Test
  void likeStateReturnsServiceUnavailableWhenClientIpIsUnavailable() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/articles/{id}/likes/me", article.getId()))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void likeStateReturnsBadRequestWhenArticleIdIsInvalid() throws Exception {
    mockMvc
        .perform(get("/api/articles/{id}/likes/me", "invalid").header("X-Forwarded-For", "1.1.1.1"))
        .andExpect(status().isBadRequest());
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

  private ArticleSource source() {
    return ArticleSource.create(
        "Example",
        "https://example.com/feed.xml",
        new SourcePolicy(new CrawlingPolicy(List.of("example.com"), List.of("article"), List.of())),
        "en",
        SourceType.RSS);
  }
}

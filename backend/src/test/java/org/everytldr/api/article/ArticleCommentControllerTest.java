package org.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.everytldr.TestcontainersConfig;
import org.everytldr.common.domain.article.Article;
import org.everytldr.common.domain.article.ArticleComment;
import org.everytldr.common.domain.article.ArticleCommentRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles({"api", "test"})
@Transactional
class ArticleCommentControllerTest {
  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleCommentRepository commentRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;

  private Category football;

  @BeforeEach
  void seedCategories() {
    football = categoryRepository.saveAndFlush(Category.create("football", 0));
  }

  @Test
  void createsAndListsThreadedComments() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    entityManager.flush();
    entityManager.clear();

    String parentJson =
        """
        {"nickname":"reader","password":"secret1234","content":"first"}
        """;
    String parentResponse =
        mockMvc
            .perform(
                post("/api/articles/{id}/comments", article.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "1.1.1.1")
                    .content(parentJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parentId").value(nullValue()))
            .andExpect(jsonPath("$.nickname").value("reader"))
            .andExpect(jsonPath("$.maskedIp").value("1.1"))
            .andExpect(jsonPath("$.content").value("first"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long parentId = Long.parseLong(parentResponse.replaceAll(".*\"id\":(\\d+).*", "$1"));
    ArticleComment parentComment = commentRepository.findById(parentId).orElseThrow();
    assertThat(parentComment.getPasswordHash()).hasSize(60);
    assertThat(BCrypt.checkpw("secret1234", parentComment.getPasswordHash())).isTrue();

    String replyJson =
        """
        {"parentId":%d,"nickname":"reply","password":"secret1234","content":"second"}
        """
            .formatted(parentId);
    mockMvc
        .perform(
            post("/api/articles/{id}/comments", article.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "1.1.1.1")
                .content(replyJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parentId").value(parentId))
        .andExpect(jsonPath("$.maskedIp").value("1.1"))
        .andExpect(jsonPath("$.content").value("second"));

    mockMvc
        .perform(get("/api/articles/{id}/comments", article.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].id").value(parentId))
        .andExpect(jsonPath("$.items[0].parentId").value(nullValue()))
        .andExpect(jsonPath("$.items[0].maskedIp").value("1.1"))
        .andExpect(jsonPath("$.items[1].parentId").value(parentId));
  }

  @Test
  void createCommentReturnsBadRequestWhenParentBelongsToAnotherArticle() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    Article otherArticle =
        saveArticle(Instant.parse("2026-04-01T01:00:00Z"), football, "ko", "다른 제목", "다른 요약");
    ArticleComment otherComment =
        commentRepository.saveAndFlush(
            ArticleComment.createTopLevel(
                otherArticle, "reader", "p".repeat(60), "c".repeat(64), "203.0", "댓글"));
    entityManager.flush();
    entityManager.clear();

    String body =
        """
        {"parentId":%d,"nickname":"reply","password":"secret1234","content":"second"}
        """
            .formatted(otherComment.getId());

    mockMvc
        .perform(
            post("/api/articles/{id}/comments", article.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "1.1.1.1")
                .content(body))
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
}

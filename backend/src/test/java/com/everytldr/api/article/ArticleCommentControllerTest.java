package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleComment;
import com.everytldr.common.domain.article.ArticleCommentRepository;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleSummary;
import com.everytldr.common.domain.article.ArticleSummaryRepository;
import com.everytldr.common.domain.category.ArticleCategory;
import com.everytldr.common.domain.category.ArticleCategoryRepository;
import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicenseInfo;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
  @Autowired private ArticleSourceRepository sourceRepository;

  private Category football;

  @BeforeEach
  void seedFixtures() {
    sourceRepository.saveAndFlush(source());
    football = categoryRepository.saveAndFlush(Category.create("football"));
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
    String parentIdValue = parentResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    long parentId = Long.parseLong(parentIdValue);
    ArticleComment parentComment = commentRepository.findById(parentId).orElseThrow();
    assertThat(parentComment.getPasswordHash()).hasSize(60);
    assertThat(BCrypt.checkpw("secret1234", parentComment.getPasswordHash())).isTrue();

    String replyJson =
        """
        {"parentId":"%d","nickname":"reply","password":"secret1234","content":"second"}
        """
            .formatted(parentId);
    mockMvc
        .perform(
            post("/api/articles/{id}/comments", article.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "1.1.1.1")
                .content(replyJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parentId").value(parentIdValue))
        .andExpect(jsonPath("$.maskedIp").value("1.1"))
        .andExpect(jsonPath("$.content").value("second"));

    mockMvc
        .perform(get("/api/articles/{id}/comments", article.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].id").value(parentIdValue))
        .andExpect(jsonPath("$.items[0].parentId").value(nullValue()))
        .andExpect(jsonPath("$.items[0].maskedIp").value("1.1"))
        .andExpect(jsonPath("$.items[1].parentId").value(parentIdValue));
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
        {"parentId":"%d","nickname":"reply","password":"secret1234","content":"second"}
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

  @Test
  void commentsReturnNotFoundWhenArticleLicenseIsUnsupportedForPublishing() throws Exception {
    Article article =
        saveArticle(
            Instant.parse("2026-04-01T00:00:00Z"),
            football,
            "ko",
            "?쒕ぉ",
            "?붿빟",
            new LicenseInfo(LicenseCode.CC_BY_SA, "4.0"));
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/articles/{id}/comments", article.getId()))
        .andExpect(status().isNotFound());

    String body =
        """
        {"nickname":"reader","password":"secret1234","content":"first"}
        """;
    mockMvc
        .perform(
            post("/api/articles/{id}/comments", article.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "1.1.1.1")
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void commentsReturnsBadRequestWhenArticleIdIsInvalid() throws Exception {
    mockMvc
        .perform(get("/api/articles/{id}/comments", "invalid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createCommentReturnsBadRequestWhenParentIdIsInvalid() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    entityManager.flush();
    entityManager.clear();

    String body =
        """
        {"parentId":"invalid","nickname":"reply","password":"secret1234","content":"second"}
        """;

    mockMvc
        .perform(
            post("/api/articles/{id}/comments", article.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "1.1.1.1")
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editsComment() throws Exception {
    Long articleId = savedArticle().getId();
    String commentId = createComment(articleId, "reader", "secret1234", "before");

    editComment(articleId, commentId, "secret1234", "after")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("after"))
        .andExpect(jsonPath("$.editedAt").value(notNullValue()));
  }

  @Test
  void editRejectsWrongPassword() throws Exception {
    Long articleId = savedArticle().getId();
    String commentId = createComment(articleId, "reader", "secret1234", "before");

    editComment(articleId, commentId, "wrongpass", "after").andExpect(status().isForbidden());
  }

  @Test
  void deleteHidesLeafButKeepsRepliedCommentAsTombstone() throws Exception {
    Long articleId = savedArticle().getId();
    String leafId = createComment(articleId, "reader", "secret1234", "leaf");
    String parentId = createComment(articleId, "reader", "secret1234", "parent");
    createReply(articleId, parentId, "reply", "secret1234", "child");

    deleteComment(articleId, leafId, "secret1234").andExpect(status().isNoContent());
    deleteComment(articleId, parentId, "secret1234").andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/articles/{id}/comments", articleId))
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].id").value(parentId))
        .andExpect(jsonPath("$.items[0].deletedAt").value(notNullValue()))
        .andExpect(jsonPath("$.items[0].content").value(nullValue()))
        .andExpect(jsonPath("$.items[1].content").value("child"));
  }

  @Test
  void verifiesPassword() throws Exception {
    Long articleId = savedArticle().getId();
    String commentId = createComment(articleId, "reader", "secret1234", "hi");

    verifyPassword(articleId, commentId, "secret1234").andExpect(status().isNoContent());
    verifyPassword(articleId, commentId, "wrongpass").andExpect(status().isForbidden());
  }

  private Article savedArticle() {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "제목", "요약");
    entityManager.flush();
    entityManager.clear();
    return article;
  }

  private ResultActions editComment(
      Long articleId, String commentId, String password, String content) throws Exception {
    return mockMvc.perform(
        patch("/api/articles/{id}/comments/{commentId}", articleId, commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"password":"%s","content":"%s"}
                """
                    .formatted(password, content)));
  }

  private ResultActions deleteComment(Long articleId, String commentId, String password)
      throws Exception {
    return mockMvc.perform(
        delete("/api/articles/{id}/comments/{commentId}", articleId, commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"password":"%s"}
                """
                    .formatted(password)));
  }

  private ResultActions verifyPassword(Long articleId, String commentId, String password)
      throws Exception {
    return mockMvc.perform(
        post("/api/articles/{id}/comments/{commentId}/password-verification", articleId, commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"password":"%s"}
                """
                    .formatted(password)));
  }

  private String createComment(Long articleId, String nickname, String password, String content)
      throws Exception {
    String body =
        """
        {"nickname":"%s","password":"%s","content":"%s"}
        """
            .formatted(nickname, password, content);
    return postComment(articleId, body);
  }

  private String createReply(
      Long articleId, String parentId, String nickname, String password, String content)
      throws Exception {
    String body =
        """
        {"parentId":"%s","nickname":"%s","password":"%s","content":"%s"}
        """
            .formatted(parentId, nickname, password, content);
    return postComment(articleId, body);
  }

  private String postComment(Long articleId, String body) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/articles/{id}/comments", articleId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "1.1.1.1")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
  }

  private Article saveArticle(
      Instant publishedAt, Category category, String language, String title, String content) {
    return saveArticle(
        publishedAt, category, language, title, content, LicenseInfo.createCcBy("4.0"));
  }

  private Article saveArticle(
      Instant publishedAt,
      Category category,
      String language,
      String title,
      String content,
      LicenseInfo licenseInfo) {
    Article article =
        articleRepository.saveAndFlush(
            Article.create(
                "https://example.com/" + System.nanoTime(),
                "Example",
                null,
                "en",
                publishedAt,
                licenseInfo));
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(article, category));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, language, title, content));
    return article;
  }

  private ArticleSource source() {
    return ArticleSource.create(
        "Example",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of("https://example.com/feed.xml"),
                List.of("example.com"),
                List.of("article"),
                List.of(),
                List.of())),
        "en",
        SourceType.RSS);
  }
}

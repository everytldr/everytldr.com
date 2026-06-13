package com.everytldr.api.article;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleComment;
import com.everytldr.common.domain.article.ArticleCommentRepository;
import com.everytldr.common.domain.article.ArticleLike;
import com.everytldr.common.domain.article.ArticleLikeRepository;
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
import java.time.temporal.ChronoUnit;
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
class ArticleControllerTest {
  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleLikeRepository likeRepository;
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
  void listCategoryPrefixMatchesExactSlugAndHyphenDescendantsOnly() throws Exception {
    Category worldConflict = categoryRepository.saveAndFlush(Category.create("world-conflict"));
    Category war = categoryRepository.findBySlug("world-conflict-war").orElseThrow();
    Category worldConflicted = categoryRepository.saveAndFlush(Category.create("world-conflicted"));
    Instant base = Instant.parse("2026-04-01T00:00:00Z");
    saveArticle(base, worldConflicted, "ko", "World Conflicted", "본문");
    saveArticle(base.minus(1, ChronoUnit.HOURS), worldConflict, "ko", "World Conflict", "본문");
    saveArticle(base.minus(2, ChronoUnit.HOURS), war, "ko", "War", "본문");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/articles")
                .header("Accept-Language", "ko")
                .param("categoryPrefix", "world-conflict")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].title").value("World Conflict"))
        .andExpect(jsonPath("$.items[1].title").value("War"));
  }

  @Test
  void listSupportsExistingFootballTeamCategoryPrefix() throws Exception {
    Category arsenal = categoryRepository.findBySlug("sport-football-epl-arsenal").orElseThrow();
    saveArticle(Instant.parse("2026-04-01T00:00:00Z"), arsenal, "ko", "EPL", "본문");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/articles")
                .header("Accept-Language", "ko")
                .param("categoryPrefix", "sport-football-epl-arsenal")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].title").value("EPL"))
        .andExpect(jsonPath("$.items[0].category").value("sport-football-epl-arsenal"));
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
        .andExpect(jsonPath("$.contentUrl").value(article.getContentUrl()))
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

  @Test
  void detailReturnsBadRequestWhenArticleIdIsInvalid() throws Exception {
    mockMvc
        .perform(get("/api/articles/{id}", "invalid").header("Accept-Language", "ko"))
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

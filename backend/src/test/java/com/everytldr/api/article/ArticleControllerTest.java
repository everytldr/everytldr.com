package com.everytldr.api.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.api.article.view.ArticleViewRedisMemoryGuard;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleComment;
import com.everytldr.common.domain.article.ArticleCommentRepository;
import com.everytldr.common.domain.article.ArticleLike;
import com.everytldr.common.domain.article.ArticleLikeRepository;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleSummary;
import com.everytldr.common.domain.article.ArticleSummaryRepository;
import com.everytldr.common.domain.briefing.Briefing;
import com.everytldr.common.domain.briefing.BriefingArticle;
import com.everytldr.common.domain.briefing.BriefingArticleRepository;
import com.everytldr.common.domain.briefing.BriefingRepository;
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
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"api", "test"})
@Transactional
class ArticleControllerTest {
  private static final String VISITOR_COOKIE_NAME = "everytldr_visitor";

  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleLikeRepository likeRepository;
  @Autowired private ArticleCommentRepository commentRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ArticleSourceRepository sourceRepository;
  @Autowired private BriefingRepository briefingRepository;
  @Autowired private BriefingArticleRepository briefingArticleRepository;
  @Autowired private StringRedisTemplate redisTemplate;
  @MockitoBean private ArticleViewRedisMemoryGuard redisMemoryGuard;

  private Category football;

  @BeforeEach
  void seedFixtures() {
    clearRedis();
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
        .andExpect(jsonPath("$.items[0].licenseCode").value("CC-BY"))
        .andExpect(jsonPath("$.items[0].licenseVersion").value("4.0"))
        .andExpect(jsonPath("$.items[0].advertisingAllowed").value(true))
        .andExpect(jsonPath("$.items[0].requiresAttribution").value(true))
        .andExpect(jsonPath("$.items[1].title").value("T1"))
        .andExpect(jsonPath("$.nextCursor").isString());
  }

  @Test
  void popularListsArticlesInRedisViewRankOrder() throws Exception {
    Article leading =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "Leading", "Summary");
    Article trailing =
        saveArticle(Instant.parse("2026-04-01T01:00:00Z"), football, "ko", "Trailing", "Summary");

    mockMvc
        .perform(post("/api/articles/{id}/views", leading.getId()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/articles/{id}/views", leading.getId())
                .cookie(visitorCookie(createAnotherVisitorId())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            post("/api/articles/{id}/views", trailing.getId())
                .cookie(visitorCookie(createAnotherVisitorId())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/articles/popular").header("Accept-Language", "ko"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].title").value("Leading"))
        .andExpect(jsonPath("$.items[1].title").value("Trailing"))
        .andExpect(jsonPath("$.nextCursor").doesNotExist());
  }

  @Test
  void popularReturnsEmptyWhenNoViewsHaveBeenRecorded() throws Exception {
    saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "Unviewed", "Summary");

    mockMvc
        .perform(get("/api/articles/popular").header("Accept-Language", "ko"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void listHidesArticlesWithUnsupportedLicenseForPublishing() throws Exception {
    Instant base = Instant.parse("2026-04-01T00:00:00Z");
    saveArticle(base, football, "ko", "Supported", "蹂몃Ц", licenseInfo());
    saveArticle(
        base.minus(1, ChronoUnit.HOURS),
        football,
        "ko",
        "Share Alike",
        "蹂몃Ц",
        new LicenseInfo(LicenseCode.CC_BY_SA, "4.0"));
    saveArticle(
        base.minus(2, ChronoUnit.HOURS),
        football,
        "ko",
        "Unknown",
        "蹂몃Ц",
        LicenseInfo.createUnknown());
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/articles").header("Accept-Language", "ko").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Supported"));
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
        .andExpect(jsonPath("$.licenseCode").value("CC-BY"))
        .andExpect(jsonPath("$.licenseVersion").value("4.0"))
        .andExpect(jsonPath("$.advertisingAllowed").value(true))
        .andExpect(jsonPath("$.requiresAttribution").value(true))
        .andExpect(jsonPath("$.category").value("football"))
        .andExpect(jsonPath("$.likeCount").value(1))
        .andExpect(jsonPath("$.commentCount").value(1))
        .andExpect(jsonPath("$.viewCount").value(0))
        .andExpect(jsonPath("$.likedByReader").doesNotExist());
  }

  @Test
  void countViewCreatesCookieAndCountsSameVisitorOnlyOnce() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "Title", "Summary");

    MvcResult first =
        mockMvc
            .perform(post("/api/articles/{id}/views", article.getId()))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""))
            .andReturn();
    String setCookie = first.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    String visitorId = extractVisitorId(setCookie);

    mockMvc
        .perform(post("/api/articles/{id}/views", article.getId()).cookie(visitorCookie(visitorId)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    mockMvc
        .perform(
            post("/api/articles/{id}/views", article.getId())
                .cookie(visitorCookie(createAnotherVisitorId())))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(setCookie)
        .startsWith(VISITOR_COOKIE_NAME + "=")
        .contains("Path=/")
        .contains("Max-Age=31536000")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("SameSite=Lax");
    mockMvc
        .perform(get("/api/articles/{id}", article.getId()).header("Accept-Language", "ko"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.viewCount").value(2));
  }

  @Test
  void countViewReturnsNotFoundWhenArticleDoesNotExist() throws Exception {
    mockMvc.perform(post("/api/articles/{id}/views", 9_999_999L)).andExpect(status().isNotFound());
  }

  @Test
  void getBriefingReturnsBriefingCoveringArticleInRequestedLanguage() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "Title", "Summary");
    LocalDate date = LocalDate.parse("2026-04-01");
    briefingRepository.saveAndFlush(Briefing.create(date, "en", "Briefing EN", "Content EN"));
    briefingRepository.saveAndFlush(Briefing.create(date, "ko", "브리핑 KO", "내용 KO"));
    briefingArticleRepository.saveAndFlush(BriefingArticle.create(date, article));
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/articles/{id}/briefing", article.getId()).header("Accept-Language", "ko"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-04-01"))
        .andExpect(jsonPath("$.title").value("브리핑 KO"));
  }

  @Test
  void getBriefingReturnsNotFoundWhenArticleNotInAnyBriefing() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "Title", "Summary");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/articles/{id}/briefing", article.getId()).header("Accept-Language", "ko"))
        .andExpect(status().isNotFound());
  }

  @Test
  void countViewReturnsServiceUnavailableWhenRedisMemoryCapacityHasBeenReached() throws Exception {
    Article article =
        saveArticle(Instant.parse("2026-04-01T00:00:00Z"), football, "ko", "Title", "Summary");
    when(redisMemoryGuard.hasReachedCapacity()).thenReturn(true);

    mockMvc
        .perform(post("/api/articles/{id}/views", article.getId()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string(""));
  }

  @Test
  void countViewReturnsNotFoundWhenArticleLicenseIsUnsupportedForPublishing() throws Exception {
    Article article =
        saveArticle(
            Instant.parse("2026-04-01T00:00:00Z"),
            football,
            "ko",
            "Title",
            "Summary",
            new LicenseInfo(LicenseCode.CC_BY_ND, "4.0"));

    mockMvc
        .perform(post("/api/articles/{id}/views", article.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void detailReturnsNotFoundWhenArticleLicenseIsUnsupportedForPublishing() throws Exception {
    Article article =
        saveArticle(
            Instant.parse("2026-04-01T00:00:00Z"),
            football,
            "ko",
            "?쒕ぉ",
            "?붿빟",
            new LicenseInfo(LicenseCode.CC_BY_ND, "4.0"));
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/articles/{id}", article.getId()).header("Accept-Language", "ko"))
        .andExpect(status().isNotFound());
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

  @Test
  void searchReturnsBadRequestWhenQueryMissing() throws Exception {
    mockMvc
        .perform(get("/api/articles/search").header("Accept-Language", "ko"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void searchReturnsBadRequestWhenQueryBlank() throws Exception {
    mockMvc
        .perform(get("/api/articles/search").header("Accept-Language", "ko").param("q", "   "))
        .andExpect(status().isBadRequest());
  }

  @Test
  void searchReturnsBadRequestWhenQueryShorterThanMinimum() throws Exception {
    mockMvc
        .perform(get("/api/articles/search").header("Accept-Language", "ko").param("q", "우"))
        .andExpect(status().isBadRequest());
  }

  private Article saveArticle(
      Instant publishedAt, Category category, String language, String title, String content) {
    return saveArticle(publishedAt, category, language, title, content, licenseInfo());
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
        SourceType.RSS,
        licenseInfo());
  }

  private void clearRedis() {
    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              connection.serverCommands().flushDb();
              return null;
            });
  }

  private static Cookie visitorCookie(String visitorId) {
    return new Cookie(VISITOR_COOKIE_NAME, visitorId);
  }

  private static String extractVisitorId(String setCookie) {
    assertThat(setCookie).startsWith(VISITOR_COOKIE_NAME + "=");
    return setCookie.substring(VISITOR_COOKIE_NAME.length() + 1, setCookie.indexOf(';'));
  }

  private static String createAnotherVisitorId() {
    byte[] bytes = new byte[32];
    bytes[0] = 1;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private LicenseInfo licenseInfo() {
    return LicenseInfo.createCcBy("4.0");
  }
}

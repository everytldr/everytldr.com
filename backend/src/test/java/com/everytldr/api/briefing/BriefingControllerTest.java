package com.everytldr.api.briefing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
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
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
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
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"api", "test"})
@Transactional
class BriefingControllerTest {
  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private BriefingRepository briefingRepository;
  @Autowired private BriefingArticleRepository briefingArticleRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
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
  void listReturnsBriefingsWithNextCursor() throws Exception {
    saveBriefing(LocalDate.parse("2026-07-20"), "Briefing 20", "브리핑 20");
    saveBriefing(LocalDate.parse("2026-07-21"), "Briefing 21", "브리핑 21");
    saveBriefing(LocalDate.parse("2026-07-22"), "Briefing 22", "브리핑 22");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/briefings").header("Accept-Language", "ko").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].date").value("2026-07-22"))
        .andExpect(jsonPath("$.items[0].title").value("브리핑 22"))
        .andExpect(jsonPath("$.items[0].excerpt").value("내용 KO"))
        .andExpect(jsonPath("$.items[1].date").value("2026-07-21"))
        .andExpect(jsonPath("$.nextCursor").value("2026-07-21"));
  }

  @Test
  void listExcerptTakesLeadParagraphAndTruncates() throws Exception {
    String lead = "L".repeat(250);
    String content = lead + "\n\nSecond paragraph should be excluded.";
    briefingRepository.saveAndFlush(
        Briefing.create(LocalDate.parse("2026-07-22"), "en", "Long", content));
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/briefings").header("Accept-Language", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].excerpt").value("L".repeat(200) + "…"));
  }

  @Test
  void listContinuesFromCursor() throws Exception {
    saveBriefing(LocalDate.parse("2026-07-20"), "Briefing 20", "브리핑 20");
    saveBriefing(LocalDate.parse("2026-07-21"), "Briefing 21", "브리핑 21");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/briefings")
                .header("Accept-Language", "en")
                .param("cursor", "2026-07-21")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Briefing 20"))
        .andExpect(jsonPath("$.nextCursor").value((Object) null));
  }

  @Test
  void listReturnsEmptyWhenNoBriefings() throws Exception {
    mockMvc
        .perform(get("/api/briefings").header("Accept-Language", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.nextCursor").value((Object) null));
  }

  @Test
  void getReturnsBriefingWithSourceArticlesInSelectionOrder() throws Exception {
    LocalDate date = LocalDate.parse("2026-07-22");
    saveBriefing(date, "Briefing EN", "브리핑 KO");
    Article first = saveArticle("First", "첫 기사");
    Article second = saveArticle("Second", "둘째 기사");
    briefingArticleRepository.saveAndFlush(BriefingArticle.create(date, first));
    briefingArticleRepository.saveAndFlush(BriefingArticle.create(date, second));
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/briefings/{date}", "2026-07-22").header("Accept-Language", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-07-22"))
        .andExpect(jsonPath("$.title").value("Briefing EN"))
        .andExpect(jsonPath("$.content").value("Content EN"))
        .andExpect(jsonPath("$.articles.length()").value(2))
        .andExpect(jsonPath("$.articles[0].title").value("First"))
        .andExpect(jsonPath("$.articles[1].title").value("Second"));
  }

  @Test
  void getResolvesLanguageFromAcceptLanguageHeader() throws Exception {
    saveBriefing(LocalDate.parse("2026-07-22"), "Briefing EN", "브리핑 KO");
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(get("/api/briefings/{date}", "2026-07-22").header("Accept-Language", "ko"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("브리핑 KO"))
        .andExpect(jsonPath("$.content").value("내용 KO"))
        .andExpect(jsonPath("$.articles").isEmpty());
  }

  @Test
  void getReturns404WhenBriefingMissing() throws Exception {
    mockMvc
        .perform(get("/api/briefings/{date}", "2026-07-22").header("Accept-Language", "en"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getRejectsInvalidDate() throws Exception {
    mockMvc
        .perform(get("/api/briefings/{date}", "not-a-date").header("Accept-Language", "en"))
        .andExpect(status().isBadRequest());
  }

  private void saveBriefing(LocalDate date, String englishTitle, String koreanTitle) {
    briefingRepository.saveAndFlush(Briefing.create(date, "en", englishTitle, "Content EN"));
    briefingRepository.saveAndFlush(Briefing.create(date, "ko", koreanTitle, "내용 KO"));
  }

  private Article saveArticle(String title, String koreanTitle) {
    Article article =
        articleRepository.saveAndFlush(
            Article.create(
                "https://example.com/" + System.nanoTime(),
                "Example",
                null,
                "en",
                Instant.parse("2026-07-22T00:00:00Z"),
                LicenseInfo.createCcBy("4.0")));
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(article, football));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, "en", title, "Summary"));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, "ko", koreanTitle, "요약"));
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
        LicenseInfo.createCcBy("4.0"));
  }
}

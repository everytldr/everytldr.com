package com.everytldr.scheduler.briefing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.article.ArticleSummary;
import com.everytldr.common.domain.article.ArticleSummaryRepository;
import com.everytldr.common.domain.briefing.Briefing;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "everytldr.briefing.generation.enabled=true")
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"scheduler", "test"})
@Transactional
class BriefingGenerationServiceTest {
  @Autowired private BriefingGenerationService generationService;
  @Autowired private BriefingRepository briefingRepository;
  @Autowired private BriefingArticleRepository briefingArticleRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ArticleSourceRepository sourceRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private BriefingGenerationClient generationClient;

  private Category football;
  private LocalDate yesterday;

  @BeforeEach
  void seedFixtures() {
    clearArticleAndBriefingTables();
    sourceRepository.saveAndFlush(source());
    football = categoryRepository.saveAndFlush(Category.create("football"));
    yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
  }

  private void clearArticleAndBriefingTables() {
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
    jdbcTemplate.update("DELETE FROM briefing_article");
    jdbcTemplate.update("DELETE FROM briefing");
    jdbcTemplate.update("DELETE FROM article_category");
    jdbcTemplate.update("DELETE FROM article_summary");
    jdbcTemplate.update("DELETE FROM article");
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
  }

  @Test
  void generatesBriefingFromYesterdayTopViewedArticles() {
    Article mostViewed = saveArticle("Most viewed", yesterday, 30L, LicenseInfo.createCcBy("4.0"));
    Article secondViewed = saveArticle("Second", yesterday, 20L, LicenseInfo.createCcBy("4.0"));
    Article thirdViewed =
        saveArticle("Third", yesterday, 10L, new LicenseInfo(LicenseCode.CC_BY_SA, "4.0"));
    saveArticle("Out of window", yesterday.minusDays(1), 99L, LicenseInfo.createCcBy("4.0"));
    saveArticle("Share alike", yesterday, 99L, new LicenseInfo(LicenseCode.CC_BY_NC_SA, "4.0"));
    saveArticle("Unpublishable", yesterday, 99L, new LicenseInfo(LicenseCode.CC_BY_ND, "4.0"));
    when(generationClient.generate(any())).thenReturn(generationResults());

    generationService.generateDailyBriefing();

    assertThat(briefingRepository.findByBriefingDateAndLanguage(yesterday, "en"))
        .hasValueSatisfying(
            briefing -> {
              assertThat(briefing.getTitle()).isEqualTo("Title EN");
              assertThat(briefing.getContent()).isEqualTo("Content EN");
            });
    assertThat(briefingRepository.findByBriefingDateAndLanguage(yesterday, "ko"))
        .hasValueSatisfying(briefing -> assertThat(briefing.getTitle()).isEqualTo("제목 KO"));
    assertThat(briefingArticleRepository.findArticleIdsByBriefingDate(yesterday))
        .containsExactly(mostViewed.getId(), secondViewed.getId(), thirdViewed.getId());

    ArgumentCaptor<BriefingGenerationClient.Request> requestCaptor =
        ArgumentCaptor.forClass(BriefingGenerationClient.Request.class);
    verify(generationClient).generate(requestCaptor.capture());
    assertThat(requestCaptor.getValue().articles())
        .extracting(BriefingGenerationClient.Request.SourceArticle::title)
        .containsExactly("Most viewed", "Second", "Third");
  }

  @Test
  void skipsWhenBriefingAlreadyExists() {
    briefingRepository.saveAndFlush(Briefing.create(yesterday, "en", "Existing", "Content"));
    saveArticle("A", yesterday, 30L, LicenseInfo.createCcBy("4.0"));
    saveArticle("B", yesterday, 20L, LicenseInfo.createCcBy("4.0"));
    saveArticle("C", yesterday, 10L, LicenseInfo.createCcBy("4.0"));

    generationService.generateDailyBriefing();

    verifyNoInteractions(generationClient);
    assertThat(briefingRepository.findByBriefingDateAndLanguage(yesterday, "ko")).isEmpty();
  }

  @Test
  void skipsWhenTooFewSourceArticles() {
    saveArticle("A", yesterday, 30L, LicenseInfo.createCcBy("4.0"));
    saveArticle("B", yesterday, 20L, LicenseInfo.createCcBy("4.0"));

    generationService.generateDailyBriefing();

    verifyNoInteractions(generationClient);
    assertThat(briefingRepository.existsByBriefingDate(yesterday)).isFalse();
  }

  private List<BriefingGenerationClient.Result> generationResults() {
    return List.of(
        new BriefingGenerationClient.Result("en", "Title EN", "Content EN"),
        new BriefingGenerationClient.Result("ko", "제목 KO", "내용 KO"));
  }

  private Article saveArticle(
      String title, LocalDate publishedDate, long viewCount, LicenseInfo licenseInfo) {
    Instant publishedAt = publishedDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    Article article =
        articleRepository.saveAndFlush(
            Article.create(
                "https://example.com/" + System.nanoTime(),
                "Example",
                null,
                "en",
                publishedAt,
                licenseInfo));
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(article, football));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, "en", title, "Summary"));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, "ko", title + " KO", "요약"));
    jdbcTemplate.update(
        "UPDATE article SET view_count = ? WHERE id = ?", viewCount, article.getId());
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

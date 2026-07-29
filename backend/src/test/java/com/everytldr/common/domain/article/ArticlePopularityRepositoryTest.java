package com.everytldr.common.domain.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class ArticlePopularityRepositoryTest {

  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ArticleSourceRepository sourceRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Category category;
  private String sourceName;

  @BeforeEach
  void setUp() {
    String suffix = Long.toString(System.nanoTime());
    category = categoryRepository.saveAndFlush(Category.create("popularity-test-" + suffix));
    sourceName = "Popularity Test " + suffix;
    sourceRepository.saveAndFlush(
        ArticleSource.create(
            sourceName,
            new SourcePolicy(
                new CrawlingPolicy(
                    List.of("https://example.com/feed-" + suffix),
                    List.of("example.com"),
                    List.of("article"),
                    List.of(),
                    List.of())),
            "en",
            SourceType.RSS,
            LicenseInfo.createCcBy("4.0")));
  }

  @Test
  void ordersByViewCountThenPublicationTimeAndExcludesUnsupportedLicenses() {
    Article first =
        saveArticle("First", Instant.parse("2026-04-01T00:00:00Z"), LicenseInfo.createCcBy("4.0"));
    Article second =
        saveArticle("Second", Instant.parse("2026-04-01T01:00:00Z"), LicenseInfo.createCcBy("4.0"));
    Article unsupported =
        saveArticle(
            "Unsupported",
            Instant.parse("2026-04-01T02:00:00Z"),
            new LicenseInfo(LicenseCode.CC_BY_SA, "4.0"));
    updateViewCount(first, 5L);
    updateViewCount(second, 5L);
    updateViewCount(unsupported, 100L);

    List<ListItemProjection> rows =
        articleRepository.findMostViewedByLanguageAndLicenseCodeIn(
            "ko", Set.of(LicenseCode.CC_BY), PageRequest.of(0, 10));

    assertThat(rows)
        .extracting(ListItemProjection::id)
        .containsExactly(second.getId(), first.getId());
  }

  private Article saveArticle(String title, Instant publishedAt, LicenseInfo licenseInfo) {
    Article article =
        articleRepository.saveAndFlush(
            Article.create(
                "https://example.com/" + System.nanoTime(),
                sourceName,
                null,
                "en",
                publishedAt,
                licenseInfo));
    articleCategoryRepository.saveAndFlush(ArticleCategory.create(article, category));
    summaryRepository.saveAndFlush(ArticleSummary.create(article, "ko", title, "Summary"));
    return article;
  }

  private void updateViewCount(Article article, long viewCount) {
    jdbcTemplate.update(
        "UPDATE article SET view_count = ? WHERE id = ?", viewCount, article.getId());
  }
}

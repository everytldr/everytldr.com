package com.everytldr.common.domain.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.ArticleRepository.SearchItemProjection;
import com.everytldr.common.domain.category.ArticleCategory;
import com.everytldr.common.domain.category.ArticleCategoryRepository;
import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicenseInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// FULLTEXT indexes only surface committed rows, so this test commits its fixtures (no
// @Transactional rollback) and deletes just the inserted rows afterwards.
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ArticleSearchRepositoryTest {
  private static final List<String> CC_BY = List.of("CC-BY");
  private static final Instant PUBLISHED_AT = Instant.parse("2026-04-01T00:00:00Z");

  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleSummaryRepository summaryRepository;
  @Autowired private ArticleCategoryRepository articleCategoryRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final List<Long> articleIds = new ArrayList<>();
  private Category category;

  @BeforeEach
  void setUp() {
    category = categoryRepository.save(Category.create("search-test-" + System.nanoTime()));
  }

  @AfterEach
  void tearDown() {
    if (!articleIds.isEmpty()) {
      String ids = articleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
      jdbcTemplate.update("DELETE FROM article_category WHERE article_id IN (" + ids + ")");
      jdbcTemplate.update("DELETE FROM article_summary WHERE article_id IN (" + ids + ")");
      jdbcTemplate.update("DELETE FROM article WHERE id IN (" + ids + ")");
    }
    jdbcTemplate.update("DELETE FROM category WHERE id = ?", category.getId());
  }

  @Test
  void matchesKoreanKeywordAndMapsRow() {
    Long id = save("우주 탐사 성공", "누리호 발사", LicenseInfo.createCcBy("4.0"));

    SearchItemProjection row = search("우주").getFirst();

    assertThat(row.getId()).isEqualTo(id);
    assertThat(row.getTitle()).isEqualTo("우주 탐사 성공");
    assertThat(row.getPublishedAt()).isEqualTo(PUBLISHED_AT);
    assertThat(row.getLicenseCode()).isEqualTo("CC-BY");
    assertThat(row.getCategorySlug()).isEqualTo(category.getSlug());
  }

  @Test
  void matchesKeywordFoundOnlyInContent() {
    save("제목", "금리 인상 결정", LicenseInfo.createCcBy("4.0"));

    assertThat(search("금리")).hasSize(1);
  }

  @Test
  void excludesUnpublishableLicenseAndOtherLanguage() {
    save("우주 탐사", "본문", LicenseInfo.createCcBy("4.0"));
    save("우주 정거장", "본문", new LicenseInfo(LicenseCode.CC_BY_SA, "4.0"));
    save(PUBLISHED_AT, "en", "우주 space", LicenseInfo.createCcBy("4.0"));

    assertThat(search("우주")).extracting(SearchItemProjection::getTitle).containsExactly("우주 탐사");
  }

  @Test
  void appliesLimitAndOffset() {
    Long newest = save(PUBLISHED_AT, "ko", "축구 국가대표", LicenseInfo.createCcBy("4.0"));
    Long middle =
        save(PUBLISHED_AT.minusSeconds(3600), "ko", "축구 일정", LicenseInfo.createCcBy("4.0"));
    Long oldest =
        save(PUBLISHED_AT.minusSeconds(7200), "ko", "축구 이적", LicenseInfo.createCcBy("4.0"));

    assertThat(search("축구", 2, 0))
        .extracting(SearchItemProjection::getId)
        .containsExactly(newest, middle);
    assertThat(search("축구", 2, 2)).extracting(SearchItemProjection::getId).containsExactly(oldest);
  }

  private List<SearchItemProjection> search(String query) {
    return search(query, 10, 0);
  }

  private List<SearchItemProjection> search(String query, int limit, int offset) {
    return articleRepository.searchByLicenseCodeIn(query, "ko", CC_BY, limit, offset);
  }

  private Long save(String title, String content, LicenseInfo license) {
    return save(PUBLISHED_AT, "ko", title, content, license);
  }

  private Long save(Instant publishedAt, String language, String title, LicenseInfo license) {
    return save(publishedAt, language, title, "본문", license);
  }

  private Long save(
      Instant publishedAt, String language, String title, String content, LicenseInfo license) {
    Article article =
        articleRepository.save(
            Article.create(
                "https://example.com/" + System.nanoTime(),
                "Global Voices",
                null,
                "en",
                publishedAt,
                license));
    articleCategoryRepository.save(ArticleCategory.create(article, category));
    summaryRepository.save(ArticleSummary.create(article, language, title, content));
    articleIds.add(article.getId());
    return article.getId();
  }
}

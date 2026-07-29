package com.everytldr.ingestor.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.common.domain.license.LicenseCode;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.source.CollectedArticle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class CollectedArticleSaveServiceTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private CollectedArticleSaveService collectedArticleSaveService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleIngestionJobRepository articleIngestionJobRepository;

  @Autowired private ArticleSourceRepository sourceRepository;

  @BeforeEach
  @AfterEach
  void cleanDatabase() {
    articleIngestionJobRepository.deleteAllInBatch();
    articleRepository.deleteAllInBatch();
    entityManager.clear();
  }

  @BeforeEach
  void seedSource() {
    source();
  }

  @Test
  void savesNewArticleAndPendingJob() {
    CollectedArticle collectedArticle =
        collectedArticle("https://www.theguardian.com/football/example");

    collectedArticleSaveService.saveNewArticles(List.of(collectedArticle));
    clearPersistenceContext();

    Article article = articleRepository.findAll().getFirst();
    assertThat(article.getContentUrl()).isEqualTo(collectedArticle.contentUrl());
    assertThat(article.getSource()).isEqualTo(collectedArticle.sourceName());
    assertThat(article.getThumbnailUrl()).isEqualTo(collectedArticle.thumbnailUrl());
    assertThat(article.getLanguage()).isEqualTo(collectedArticle.language());
    assertThat(article.getPublishedAt()).isEqualTo(collectedArticle.publishedAt());
    assertThat(article.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY);
    assertThat(article.getLicenseInfo().getLicenseVersion()).isEqualTo("4.0");

    ArticleIngestionJob job =
        articleIngestionJobRepository.findByArticleId(article.getId()).orElseThrow();
    assertThat(job.getState()).isEqualTo(IngestionState.PENDING);
    assertThat(job.getUrlHash()).containsExactly(sha256(collectedArticle.contentUrl()));
  }

  @Test
  void deduplicatesSameUrlWithinBatch() {
    String url = "https://www.theguardian.com/football/duplicate";

    collectedArticleSaveService.saveNewArticles(
        List.of(collectedArticle(url), collectedArticle(url)));
    clearPersistenceContext();

    assertThat(articleRepository.findAll()).hasSize(1);
    assertThat(articleIngestionJobRepository.findAll()).hasSize(1);
  }

  @Test
  void skipsAlreadyExistingUrlHash() {
    String url = "https://www.theguardian.com/football/existing";
    Article existingArticle =
        articleRepository.saveAndFlush(
            Article.create(
                url, "The Guardian Football", null, "en", Instant.parse("2026-05-04T10:15:30Z")));
    articleIngestionJobRepository.saveAndFlush(
        ArticleIngestionJob.create(existingArticle, sha256(url)));
    clearPersistenceContext();

    collectedArticleSaveService.saveNewArticles(List.of(collectedArticle(url)));
    clearPersistenceContext();

    assertThat(articleRepository.findAll()).hasSize(1);
    assertThat(articleIngestionJobRepository.findAll()).hasSize(1);
  }

  @Test
  void savesOnlyNewArticlesWhenExistingAndNewUrlsAreMixed() {
    String existingUrl = "https://www.theguardian.com/football/existing-mixed";
    String newUrl = "https://www.theguardian.com/football/new-mixed";
    Article existingArticle =
        articleRepository.saveAndFlush(
            Article.create(
                existingUrl,
                "The Guardian Football",
                null,
                "en",
                Instant.parse("2026-05-04T10:15:30Z")));
    articleIngestionJobRepository.saveAndFlush(
        ArticleIngestionJob.create(existingArticle, sha256(existingUrl)));
    clearPersistenceContext();

    collectedArticleSaveService.saveNewArticles(
        List.of(collectedArticle(existingUrl), collectedArticle(newUrl)));
    clearPersistenceContext();

    assertThat(articleRepository.findAll())
        .extracting(Article::getContentUrl)
        .containsExactlyInAnyOrder(existingUrl, newUrl);
    assertThat(articleIngestionJobRepository.findAll()).hasSize(2);
  }

  @Test
  void skipsInvalidCollectedArticles() {
    String validUrl = "https://www.theguardian.com/football/valid";

    collectedArticleSaveService.saveNewArticles(
        List.of(
            collectedArticle(""),
            new CollectedArticle(validUrl, "", null, "en", Instant.parse("2026-05-04T10:15:30Z")),
            new CollectedArticle(
                validUrl + "/missing-language",
                "The Guardian Football",
                null,
                "",
                Instant.parse("2026-05-04T10:15:30Z")),
            new CollectedArticle(
                validUrl + "/missing-published-at", "The Guardian Football", null, "en", null),
            collectedArticle("javascript:alert(1)"),
            new CollectedArticle(
                validUrl + "/bad-thumbnail",
                "The Guardian Football",
                "file:///etc/passwd",
                "en",
                Instant.parse("2026-05-04T10:15:30Z"))));
    clearPersistenceContext();

    assertThat(articleRepository.findAll()).isEmpty();
    assertThat(articleIngestionJobRepository.findAll()).isEmpty();
  }

  @Test
  void skipsCollectedArticlesWithUnsupportedLicenseForPublishing() {
    collectedArticleSaveService.saveNewArticles(
        List.of(
            collectedArticle(
                "https://www.theguardian.com/football/unknown-license",
                LicenseInfo.createUnknown()),
            collectedArticle(
                "https://www.theguardian.com/football/non-commercial-no-derivatives",
                new LicenseInfo(LicenseCode.CC_BY_NC_ND, "4.0")),
            collectedArticle(
                "https://www.theguardian.com/football/no-derivatives",
                new LicenseInfo(LicenseCode.CC_BY_ND, "4.0"))));
    clearPersistenceContext();

    assertThat(articleRepository.findAll()).isEmpty();
    assertThat(articleIngestionJobRepository.findAll()).isEmpty();
  }

  @Test
  void savesCollectedArticleWithNonCommercialTransformableLicense() {
    CollectedArticle collectedArticle =
        collectedArticle(
            "https://www.theguardian.com/football/non-commercial",
            new LicenseInfo(LicenseCode.CC_BY_NC, "4.0"));

    collectedArticleSaveService.saveNewArticles(List.of(collectedArticle));
    clearPersistenceContext();

    Article article = articleRepository.findAll().getFirst();
    assertThat(article.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY_NC);
    assertThat(articleIngestionJobRepository.findAll()).hasSize(1);
  }

  @Test
  void savesCollectedArticleWithShareAlikeTransformableLicense() {
    CollectedArticle collectedArticle =
        collectedArticle(
            "https://www.theguardian.com/football/share-alike",
            new LicenseInfo(LicenseCode.CC_BY_NC_SA, "4.0"));

    collectedArticleSaveService.saveNewArticles(List.of(collectedArticle));
    clearPersistenceContext();

    Article article = articleRepository.findAll().getFirst();
    assertThat(article.getLicenseInfo().getLicenseCode()).isEqualTo(LicenseCode.CC_BY_NC_SA);
    assertThat(articleIngestionJobRepository.findAll()).hasSize(1);
  }

  private CollectedArticle collectedArticle(String sourceUrl) {
    return collectedArticle(sourceUrl, licenseInfo());
  }

  private CollectedArticle collectedArticle(String sourceUrl, LicenseInfo licenseInfo) {
    return new CollectedArticle(
        sourceUrl,
        "The Guardian Football",
        "https://media.guim.co.uk/example-thumbnail.jpg",
        "en",
        Instant.parse("2026-05-04T10:15:30Z"),
        licenseInfo);
  }

  private ArticleSource source() {
    return sourceRepository
        .findByName("The Guardian Football")
        .orElseGet(
            () ->
                sourceRepository.saveAndFlush(
                    ArticleSource.create(
                        "The Guardian Football",
                        new SourcePolicy(
                            new CrawlingPolicy(
                                List.of("https://example.com/rss.xml"),
                                List.of("theguardian.com"),
                                List.of("article"),
                                List.of(),
                                List.of())),
                        "en",
                        SourceType.RSS,
                        licenseInfo())));
  }

  private LicenseInfo licenseInfo() {
    return LicenseInfo.createCcBy("4.0");
  }

  private void clearPersistenceContext() {
    entityManager.clear();
  }

  private byte[] sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}

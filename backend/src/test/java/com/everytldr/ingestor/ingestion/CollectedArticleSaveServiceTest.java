package com.everytldr.ingestor.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.ingestion.IngestionState;
import com.everytldr.ingestor.provider.CollectedArticle;
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

  @BeforeEach
  @AfterEach
  void cleanDatabase() {
    articleIngestionJobRepository.deleteAllInBatch();
    articleRepository.deleteAllInBatch();
    entityManager.clear();
  }

  @Test
  void savesNewArticleAndPendingJob() {
    CollectedArticle collectedArticle =
        collectedArticle("https://www.theguardian.com/football/example");

    collectedArticleSaveService.saveNewArticles(List.of(collectedArticle));
    clearPersistenceContext();

    Article article = articleRepository.findAll().getFirst();
    assertThat(article.getSourceUrl()).isEqualTo(collectedArticle.sourceUrl());
    assertThat(article.getSource()).isEqualTo(collectedArticle.sourceName());
    assertThat(article.getThumbnailUrl()).isEqualTo(collectedArticle.thumbnailUrl());
    assertThat(article.getLanguage()).isEqualTo(collectedArticle.language());
    assertThat(article.getPublishedAt()).isEqualTo(collectedArticle.publishedAt());

    ArticleIngestionJob job =
        articleIngestionJobRepository.findByArticleId(article.getId()).orElseThrow();
    assertThat(job.getState()).isEqualTo(IngestionState.PENDING);
    assertThat(job.getUrlHash()).containsExactly(sha256(collectedArticle.sourceUrl()));
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
        .extracting(Article::getSourceUrl)
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

  private CollectedArticle collectedArticle(String sourceUrl) {
    return new CollectedArticle(
        sourceUrl,
        "The Guardian Football",
        "https://media.guim.co.uk/example-thumbnail.jpg",
        "en",
        Instant.parse("2026-05-04T10:15:30Z"));
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

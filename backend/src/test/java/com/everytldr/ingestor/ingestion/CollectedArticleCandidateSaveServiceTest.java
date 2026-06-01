package com.everytldr.ingestor.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class CollectedArticleCandidateSaveServiceTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private CollectedArticleCandidateSaveService collectedArticleCandidateSaveService;

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
  void savesArticleAndPendingJobInNewTransaction() {
    CollectedArticle collectedArticle =
        collectedArticle("https://www.theguardian.com/football/candidate");

    collectedArticleCandidateSaveService.saveNewArticleCandidate(
        collectedArticle, sha256(collectedArticle.sourceUrl()));
    entityManager.clear();

    Article article = articleRepository.findAll().getFirst();
    assertThat(article.getSourceUrl()).isEqualTo(collectedArticle.sourceUrl());

    ArticleIngestionJob job =
        articleIngestionJobRepository.findByArticleId(article.getId()).orElseThrow();
    assertThat(job.getState()).isEqualTo(IngestionState.PENDING);
    assertThat(job.getUrlHash()).containsExactly(sha256(collectedArticle.sourceUrl()));
  }

  @Test
  void rollsBackArticleWhenUrlHashAlreadyExists() {
    String existingUrl = "https://www.theguardian.com/football/existing-candidate";
    String conflictingUrl = "https://www.theguardian.com/football/conflicting-candidate";
    byte[] existingUrlHash = sha256(existingUrl);
    Article existingArticle =
        articleRepository.saveAndFlush(
            Article.create(
                existingUrl,
                "The Guardian Football",
                null,
                "en",
                Instant.parse("2026-05-04T10:15:30Z")));
    articleIngestionJobRepository.saveAndFlush(
        ArticleIngestionJob.create(existingArticle, existingUrlHash));
    entityManager.clear();

    assertThatThrownBy(
            () ->
                collectedArticleCandidateSaveService.saveNewArticleCandidate(
                    collectedArticle(conflictingUrl), existingUrlHash))
        .isInstanceOf(DataIntegrityViolationException.class);
    entityManager.clear();

    assertThat(articleRepository.findAll())
        .extracting(Article::getSourceUrl)
        .containsOnly(existingUrl);
    assertThat(articleIngestionJobRepository.findAll()).hasSize(1);
  }

  private CollectedArticle collectedArticle(String sourceUrl) {
    return new CollectedArticle(
        sourceUrl,
        "The Guardian Football",
        "https://media.guim.co.uk/example-thumbnail.jpg",
        "en",
        Instant.parse("2026-05-04T10:15:30Z"));
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

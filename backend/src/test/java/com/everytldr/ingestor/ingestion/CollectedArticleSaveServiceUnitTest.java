package com.everytldr.ingestor.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import com.everytldr.ingestor.source.CollectedArticle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class CollectedArticleSaveServiceUnitTest {

  @Test
  void continuesWithNextCandidateWhenOneCandidateSaveConflicts() {
    ArticleIngestionJobRepository articleIngestionJobRepository =
        mock(ArticleIngestionJobRepository.class);
    CollectedArticleCandidateSaveService collectedArticleCandidateSaveService =
        mock(CollectedArticleCandidateSaveService.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    CollectedArticleSaveService collectedArticleSaveService =
        new CollectedArticleSaveService(
            articleIngestionJobRepository,
            collectedArticleCandidateSaveService,
            new LicensePolicyEvaluator(),
            new IngestionMetrics(meterRegistry));
    CollectedArticle conflictingArticle =
        collectedArticle("https://www.theguardian.com/football/race-conflict");
    CollectedArticle newArticle = collectedArticle("https://www.theguardian.com/football/new");
    when(articleIngestionJobRepository.findExistingUrlHashes(anyCollection()))
        .thenReturn(List.of());
    doThrow(new DataIntegrityViolationException("Duplicate url hash"))
        .when(collectedArticleCandidateSaveService)
        .saveNewArticleCandidate(eq(conflictingArticle), any(byte[].class));

    collectedArticleSaveService.saveNewArticles(List.of(conflictingArticle, newArticle));

    verify(collectedArticleCandidateSaveService)
        .saveNewArticleCandidate(eq(conflictingArticle), any(byte[].class));
    verify(collectedArticleCandidateSaveService)
        .saveNewArticleCandidate(eq(newArticle), any(byte[].class));
    assertThat(articleMetricCount(meterRegistry, "received")).isEqualTo(2.0);
    assertThat(articleMetricCount(meterRegistry, "valid")).isEqualTo(2.0);
    assertThat(articleMetricCount(meterRegistry, "concurrency_duplicate_skipped")).isEqualTo(1.0);
    assertThat(articleMetricCount(meterRegistry, "saved")).isEqualTo(1.0);
  }

  @Test
  void recordsArticleMetrics() {
    ArticleIngestionJobRepository articleIngestionJobRepository =
        mock(ArticleIngestionJobRepository.class);
    CollectedArticleCandidateSaveService collectedArticleCandidateSaveService =
        mock(CollectedArticleCandidateSaveService.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    CollectedArticleSaveService collectedArticleSaveService =
        new CollectedArticleSaveService(
            articleIngestionJobRepository,
            collectedArticleCandidateSaveService,
            new LicensePolicyEvaluator(),
            new IngestionMetrics(meterRegistry));
    String existingUrl = "https://www.theguardian.com/football/existing";
    String newUrl = "https://www.theguardian.com/football/new";
    when(articleIngestionJobRepository.findExistingUrlHashes(anyCollection()))
        .thenReturn(List.of(sha256(existingUrl)));

    collectedArticleSaveService.saveNewArticles(
        List.of(
            collectedArticle(existingUrl),
            collectedArticle(newUrl),
            collectedArticle(newUrl),
            collectedArticle("")));

    assertThat(articleMetricCount(meterRegistry, "received")).isEqualTo(4.0);
    assertThat(articleMetricCount(meterRegistry, "valid")).isEqualTo(2.0);
    assertThat(articleMetricCount(meterRegistry, "invalid_skipped")).isEqualTo(1.0);
    assertThat(articleMetricCount(meterRegistry, "duplicate_in_batch_skipped")).isEqualTo(1.0);
    assertThat(articleMetricCount(meterRegistry, "existing_duplicate_skipped")).isEqualTo(1.0);
    assertThat(articleMetricCount(meterRegistry, "saved")).isEqualTo(1.0);
  }

  private double articleMetricCount(SimpleMeterRegistry meterRegistry, String result) {
    return meterRegistry.get("everytldr.ingestor.articles").tag("result", result).counter().count();
  }

  private CollectedArticle collectedArticle(String sourceUrl) {
    return new CollectedArticle(
        sourceUrl,
        "The Guardian Football",
        "https://media.guim.co.uk/example-thumbnail.jpg",
        "en",
        Instant.parse("2026-05-04T10:15:30Z"),
        LicenseInfo.createCcBy("4.0"));
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

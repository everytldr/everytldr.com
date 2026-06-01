package com.everytldr.ingestor.ingestion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.ingestor.provider.CollectedArticle;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class CollectedArticleSaveServiceUnitTest {

  @Test
  void continuesWithNextCandidateWhenOneCandidateSaveConflicts() {
    ArticleIngestionJobRepository articleIngestionJobRepository =
        mock(ArticleIngestionJobRepository.class);
    CollectedArticleCandidateSaveService collectedArticleCandidateSaveService =
        mock(CollectedArticleCandidateSaveService.class);
    CollectedArticleSaveService collectedArticleSaveService =
        new CollectedArticleSaveService(
            articleIngestionJobRepository, collectedArticleCandidateSaveService);
    CollectedArticle conflictingArticle =
        collectedArticle("https://www.theguardian.com/football/race-conflict");
    CollectedArticle newArticle = collectedArticle("https://www.theguardian.com/football/new");
    when(articleIngestionJobRepository.findExistingUrlHashes(anyCollection()))
        .thenReturn(List.of());
    doThrow(new DataIntegrityViolationException("Duplicate url hash"))
        .when(collectedArticleCandidateSaveService)
        .saveNewArticleCandidate(eq(conflictingArticle), any(byte[].class));

    assertThatCode(
            () ->
                collectedArticleSaveService.saveNewArticles(
                    List.of(conflictingArticle, newArticle)))
        .doesNotThrowAnyException();

    verify(collectedArticleCandidateSaveService)
        .saveNewArticleCandidate(eq(conflictingArticle), any(byte[].class));
    verify(collectedArticleCandidateSaveService)
        .saveNewArticleCandidate(eq(newArticle), any(byte[].class));
  }

  private CollectedArticle collectedArticle(String sourceUrl) {
    return new CollectedArticle(
        sourceUrl,
        "The Guardian Football",
        "https://media.guim.co.uk/example-thumbnail.jpg",
        "en",
        Instant.parse("2026-05-04T10:15:30Z"));
  }
}

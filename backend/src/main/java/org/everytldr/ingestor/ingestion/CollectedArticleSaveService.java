package org.everytldr.ingestor.ingestion;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import org.everytldr.ingestor.provider.CollectedArticle;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectedArticleSaveService {

  private static final int MAX_URL_LENGTH = 1000;
  private static final int MAX_SOURCE_NAME_LENGTH = 100;
  private static final int MAX_LANGUAGE_LENGTH = 10;

  private record ArticleCandidate(
      CollectedArticle collectedArticle, byte[] urlHash, String urlHashHex) {}

  private final ArticleIngestionJobRepository articleIngestionJobRepository;

  private final CollectedArticleCandidateSaveService collectedArticleCandidateSaveService;

  public void saveNewArticles(List<CollectedArticle> collectedArticles) {
    int receivedCount = collectedArticles == null ? 0 : collectedArticles.size();
    if (receivedCount == 0) {
      log.info(
          "Finished saving collected articles. received=0, valid=0, invalidSkipped=0, duplicateInBatchSkipped=0, existingDuplicateSkipped=0, concurrencyDuplicateSkipped=0, saved=0");
      return;
    }

    Set<String> seenUrls = new HashSet<>();
    List<CollectedArticle> validArticles = new ArrayList<>();
    int invalidSkippedCount = 0;
    int duplicateInBatchSkippedCount = 0;

    for (CollectedArticle article : collectedArticles) {
      if (!isValid(article)) {
        invalidSkippedCount++;
        continue;
      }
      if (seenUrls.add(article.sourceUrl())) {
        validArticles.add(article);
      } else {
        duplicateInBatchSkippedCount++;
      }
    }

    if (validArticles.isEmpty()) {
      log.info(
          "Finished saving collected articles. received={}, valid=0, invalidSkipped={}, duplicateInBatchSkipped={}, existingDuplicateSkipped=0, concurrencyDuplicateSkipped=0, saved=0",
          receivedCount,
          invalidSkippedCount,
          duplicateInBatchSkippedCount);
      return;
    }

    List<ArticleCandidate> candidates = new ArrayList<>();
    for (CollectedArticle article : validArticles) {
      byte[] urlHash = sha256(article.sourceUrl());
      candidates.add(new ArticleCandidate(article, urlHash, toHex(urlHash)));
    }

    List<byte[]> urlHashes = candidates.stream().map(candidate -> candidate.urlHash).toList();
    List<byte[]> existingUrlHashes = articleIngestionJobRepository.findExistingUrlHashes(urlHashes);
    Set<String> existingHashHexes =
        existingUrlHashes.stream().map(this::toHex).collect(Collectors.toSet());

    int existingDuplicateSkippedCount = 0;
    int concurrencyDuplicateSkippedCount = 0;
    int savedCount = 0;

    for (ArticleCandidate candidate : candidates) {
      if (existingHashHexes.contains(candidate.urlHashHex)) {
        existingDuplicateSkippedCount++;
        continue;
      }

      try {
        collectedArticleCandidateSaveService.saveNewArticleCandidate(
            candidate.collectedArticle, candidate.urlHash);
        savedCount++;
      } catch (DataIntegrityViolationException e) {
        concurrencyDuplicateSkippedCount++;
        log.info(
            "Skipped article candidate because article ingestion job already exists. urlHash={}",
            candidate.urlHashHex);
      }
    }

    log.info(
        "Finished saving collected articles. received={}, valid={}, invalidSkipped={}, duplicateInBatchSkipped={}, existingDuplicateSkipped={}, concurrencyDuplicateSkipped={}, saved={}",
        receivedCount,
        validArticles.size(),
        invalidSkippedCount,
        duplicateInBatchSkippedCount,
        existingDuplicateSkippedCount,
        concurrencyDuplicateSkippedCount,
        savedCount);
  }

  private boolean isValid(CollectedArticle article) {
    return article != null
        && isRequiredHttpUrl(article.sourceUrl())
        && hasRequiredText(article.sourceName(), MAX_SOURCE_NAME_LENGTH)
        && hasRequiredText(article.language(), MAX_LANGUAGE_LENGTH)
        && article.publishedAt() != null
        && isOptionalHttpUrl(article.thumbnailUrl());
  }

  private boolean isRequiredHttpUrl(String value) {
    return hasRequiredText(value, MAX_URL_LENGTH) && isHttpUrl(value);
  }

  private boolean isOptionalHttpUrl(String value) {
    return value == null || isRequiredHttpUrl(value);
  }

  private boolean hasRequiredText(String value, int maxLength) {
    return value != null && !value.isBlank() && value.length() <= maxLength;
  }

  private boolean isHttpUrl(String value) {
    try {
      URI uri = new URI(value);
      String scheme = uri.getScheme();
      return uri.getHost() != null
          && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private byte[] sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }

  private String toHex(byte[] bytes) {
    return HexFormat.of().formatHex(bytes);
  }
}

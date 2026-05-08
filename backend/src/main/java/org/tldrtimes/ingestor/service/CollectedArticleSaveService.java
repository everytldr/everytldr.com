package org.tldrtimes.ingestor.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJobRepository;
import org.tldrtimes.ingestor.provider.CollectedArticle;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectedArticleSaveService {

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
    if (article == null) return false;
    if (article.sourceUrl() == null
        || article.sourceUrl().isBlank()
        || article.sourceUrl().length() > 1000) return false;
    if (!isHttpUrl(article.sourceUrl())) return false;
    if (article.sourceName() == null
        || article.sourceName().isBlank()
        || article.sourceName().length() > 100) return false;
    if (article.language() == null
        || article.language().isBlank()
        || article.language().length() > 10) return false;
    if (article.publishedAt() == null) return false;
    if (article.thumbnailUrl() != null && article.thumbnailUrl().length() > 1000) return false;
    if (article.thumbnailUrl() != null && !isHttpUrl(article.thumbnailUrl())) return false;
    return true;
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

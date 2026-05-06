package org.tldrtimes.ingestor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.common.domain.article.Article;
import org.tldrtimes.common.domain.article.ArticleRepository;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJob;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJobRepository;
import org.tldrtimes.ingestor.provider.CollectedArticle;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectedArticleSaveService {

  private record ArticleCandidate(
      CollectedArticle collectedArticle, byte[] urlHash, String urlHashHex) {}

  private final ArticleRepository articleRepository;

  private final ArticleIngestionJobRepository articleIngestionJobRepository;

  @Transactional
  public void saveNewArticles(List<CollectedArticle> collectedArticles) {
    if (collectedArticles == null || collectedArticles.isEmpty()) return;

    Set<String> seenUrls = new HashSet<>();
    List<CollectedArticle> validArticles = new ArrayList<>();

    for (CollectedArticle article : collectedArticles) {
      if (!isValid(article)) continue;
      if (seenUrls.add(article.sourceUrl())) {
        validArticles.add(article);
      }
    }

    if (validArticles.isEmpty()) return;

    List<ArticleCandidate> candidates = new ArrayList<>();
    for (CollectedArticle article : validArticles) {
      byte[] urlHash = sha256(article.sourceUrl());
      candidates.add(new ArticleCandidate(article, urlHash, toHex(urlHash)));
    }

    List<byte[]> urlHashes = candidates.stream().map(candidate -> candidate.urlHash).toList();
    List<byte[]> existingUrlHashes = articleIngestionJobRepository.findExistingUrlHashes(urlHashes);
    Set<String> existingHashHexes =
        existingUrlHashes.stream().map(this::toHex).collect(Collectors.toSet());

    for (ArticleCandidate candidate : candidates) {
      if (existingHashHexes.contains(candidate.urlHashHex)) continue;

      Article article =
          Article.create(
              candidate.collectedArticle.sourceUrl(),
              candidate.collectedArticle.sourceName(),
              candidate.collectedArticle.thumbnailUrl(),
              candidate.collectedArticle.language(),
              candidate.collectedArticle.publishedAt());
      Article savedArticle = articleRepository.save(article);

      ArticleIngestionJob job = ArticleIngestionJob.create(savedArticle, candidate.urlHash);
      articleIngestionJobRepository.save(job);
    }
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

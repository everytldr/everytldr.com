package org.tldrtimes.ingestor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.common.domain.article.Article;
import org.tldrtimes.common.domain.article.ArticleRepository;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJob;
import org.tldrtimes.common.domain.ingestion.ArticleIngestionJobRepository;
import org.tldrtimes.ingestor.provider.CollectedArticle;

@Service
@RequiredArgsConstructor
public class CollectedArticleCandidateSaveService {

  private final ArticleRepository articleRepository;

  private final ArticleIngestionJobRepository articleIngestionJobRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveNewArticleCandidate(CollectedArticle collectedArticle, byte[] urlHash) {
    Article article =
        Article.create(
            collectedArticle.sourceUrl(),
            collectedArticle.sourceName(),
            collectedArticle.thumbnailUrl(),
            collectedArticle.language(),
            collectedArticle.publishedAt());
    Article savedArticle = articleRepository.save(article);

    ArticleIngestionJob job = ArticleIngestionJob.create(savedArticle, urlHash);
    articleIngestionJobRepository.saveAndFlush(job);
  }
}

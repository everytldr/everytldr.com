package com.everytldr.ingestor.ingestion;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleRepository;
import com.everytldr.common.domain.ingestion.ArticleIngestionJob;
import com.everytldr.common.domain.ingestion.ArticleIngestionJobRepository;
import com.everytldr.ingestor.source.CollectedArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectedArticleCandidateSaveService {

  private final ArticleRepository articleRepository;

  private final ArticleIngestionJobRepository articleIngestionJobRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveNewArticleCandidate(CollectedArticle collectedArticle, byte[] urlHash) {
    Article article =
        Article.create(
            collectedArticle.contentUrl(),
            collectedArticle.sourceName(),
            collectedArticle.thumbnailUrl(),
            collectedArticle.language(),
            collectedArticle.publishedAt(),
            collectedArticle.licenseInfo());
    Article savedArticle = articleRepository.save(article);

    ArticleIngestionJob job = ArticleIngestionJob.create(savedArticle, urlHash);
    articleIngestionJobRepository.saveAndFlush(job);
  }
}

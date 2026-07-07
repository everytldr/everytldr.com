package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.ingestion.CollectedArticleSaveService;
import com.everytldr.ingestor.source.CollectedArticle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectedArticleWriter implements ItemWriter<ArticleCollectionResult> {
  private final CollectedArticleSaveService collectedArticleSaveService;

  @Override
  public void write(@NonNull Chunk<? extends ArticleCollectionResult> chunk) throws Exception {
    List<CollectedArticle> articles =
        chunk.getItems().stream().flatMap(result -> result.collectedArticles().stream()).toList();
    if (articles.isEmpty()) {
      return;
    }
    collectedArticleSaveService.saveNewArticles(articles);
  }
}

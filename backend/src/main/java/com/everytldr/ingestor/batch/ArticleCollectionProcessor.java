package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.ingestion.IngestionExceptions;
import com.everytldr.ingestor.ingestion.IngestionMetrics;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.everytldr.ingestor.source.SourceClientRegistry;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCollectionProcessor
    implements ItemProcessor<ArticleCollectionTarget, ArticleCollectionResult> {

  private final SourceClientRegistry sourceClientRegistry;

  private final IngestionMetrics ingestionMetrics;

  @Override
  public @Nullable ArticleCollectionResult process(ArticleCollectionTarget item) throws Exception {
    long startedAt = System.nanoTime();
    try {
      SourceClient client = sourceClientRegistry.getClient(item.source().getSourceType());
      List<CollectedArticle> collectedArticles = client.collect(item);
      recordTargetAttemptOutcome(item, "success", startedAt);
      return new ArticleCollectionResult(item, collectedArticles);
    } catch (IngestionExceptions.Retryable e) {
      recordTargetAttemptOutcome(item, "retryable_failure", startedAt);
      throw e;
    } catch (IngestionExceptions.Skippable e) {
      recordTargetAttemptOutcome(item, "skippable_failure", startedAt);
      throw e;
    } catch (RuntimeException e) {
      recordTargetAttemptOutcome(item, "fatal_failure", startedAt);
      throw new IngestionExceptions.Fatal(
          "Failed to collect articles. sourceName=%s, feedUrl=%s"
              .formatted(item.source().getName(), item.feedUrl()),
          e);
    }
  }

  private void recordTargetAttemptOutcome(
      ArticleCollectionTarget item, String outcome, long startedAt) {
    Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
    ingestionMetrics.recordArticleCollectionTargetAttempt(item.source().getSourceType(), outcome);
    ingestionMetrics.recordArticleCollectionTargetAttemptDuration(
        item.source().getSourceType(), outcome, duration);
  }
}

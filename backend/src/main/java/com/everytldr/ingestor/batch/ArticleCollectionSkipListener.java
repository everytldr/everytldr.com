package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.source.ArticleCollectionTarget;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArticleCollectionSkipListener
    implements SkipListener<ArticleCollectionTarget, ArticleCollectionResult> {

  @Override
  public void onSkipInRead(@NonNull Throwable t) {
    log.warn(
        "Skipped article collection target while reading. exceptionType={}",
        findExceptionType(t),
        t);
  }

  @Override
  public void onSkipInProcess(ArticleCollectionTarget item, @NonNull Throwable t) {
    log.warn(
        "Skipped article collection target while processing. sourceId={}, sourceName={}, sourceType={}, feedUrl={}, exceptionType={}",
        item.source().getId(),
        item.source().getName(),
        item.source().getSourceType(),
        item.feedUrl(),
        findExceptionType(t),
        t);
  }

  @Override
  public void onSkipInWrite(ArticleCollectionResult item, @NonNull Throwable t) {
    ArticleCollectionTarget target = item.target();
    log.warn(
        "Skipped article collection result while writing. sourceId={}, sourceName={}, sourceType={}, feedUrl={}, exceptionType={}",
        target.source().getId(),
        target.source().getName(),
        target.source().getSourceType(),
        target.feedUrl(),
        findExceptionType(t),
        t);
  }

  private String findExceptionType(Throwable t) {
    return t == null ? "unknown" : t.getClass().getSimpleName();
  }
}

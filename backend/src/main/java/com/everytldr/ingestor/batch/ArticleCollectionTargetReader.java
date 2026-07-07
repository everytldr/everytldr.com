package com.everytldr.ingestor.batch;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class ArticleCollectionTargetReader implements ItemStreamReader<ArticleCollectionTarget> {

  static final String NEXT_INDEX_KEY = "articleCollectionTargetReader.nextIndex";

  private final ArticleSourceRepository articleSourceRepository;

  private List<ArticleCollectionTarget> targets = List.of();

  private int nextIndex;

  static void resetSavedNextIndex(ExecutionContext executionContext) {
    Objects.requireNonNull(executionContext, "executionContext must not be null");
    executionContext.putInt(NEXT_INDEX_KEY, 0);
  }

  @Override
  public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
    Objects.requireNonNull(executionContext, "executionContext must not be null");
    targets = collectTargets();
    nextIndex = executionContext.getInt(NEXT_INDEX_KEY, 0);
    if (nextIndex < 0 || nextIndex > targets.size()) {
      throw new ItemStreamException(
          "Saved article collection target index is out of range. nextIndex=%d, targets=%d"
              .formatted(nextIndex, targets.size()));
    }
  }

  @Override
  public @Nullable ArticleCollectionTarget read() {
    if (nextIndex >= targets.size()) {
      return null;
    }
    return targets.get(nextIndex++);
  }

  @Override
  public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
    Objects.requireNonNull(executionContext, "executionContext must not be null");
    executionContext.putInt(NEXT_INDEX_KEY, nextIndex);
  }

  @Override
  public void close() throws ItemStreamException {
    targets = List.of();
    nextIndex = 0;
  }

  private List<ArticleCollectionTarget> collectTargets() {
    List<ArticleCollectionTarget> collectedTargets = new ArrayList<>();
    List<ArticleSource> activeSources = articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc();
    for (ArticleSource source : activeSources) {
      if (source == null) {
        continue;
      }

      for (String feedUrl : source.getPolicy().crawling().feedUrls()) {
        collectedTargets.add(new ArticleCollectionTarget(source, feedUrl));
      }
    }
    return List.copyOf(collectedTargets);
  }
}

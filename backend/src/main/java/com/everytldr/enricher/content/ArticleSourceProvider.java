package com.everytldr.enricher.content;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("enricher")
public class ArticleSourceProvider {
  public static final String CACHE_NAME = "enricherArticleSources";

  private final ArticleSourceRepository articleSourceRepository;

  @Cacheable(cacheNames = CACHE_NAME, sync = true)
  public Optional<ArticleSource> findByName(String name) {
    return articleSourceRepository.findByName(name);
  }
}

package org.everytldr.enricher;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.everytldr.enricher.enrichment.ArticleEnrichmentCategoryOptionProvider;
import org.everytldr.enricher.enrichment.EnricherCategoryOptionsCacheProperties;
import org.everytldr.enricher.enrichment.EnricherContentProperties;
import org.everytldr.enricher.enrichment.gemini.EnricherGeminiProperties;
import org.everytldr.enricher.processing.EnricherProcessingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties({
  EnricherProcessingProperties.class,
  EnricherContentProperties.class,
  EnricherGeminiProperties.class,
  EnricherCategoryOptionsCacheProperties.class
})
@EnableCaching
@EnableScheduling
@Profile("enricher")
public class EnricherConfig {
  @Bean
  CacheManager enricherCacheManager(EnricherCategoryOptionsCacheProperties properties) {
    CaffeineCacheManager cacheManager =
        new CaffeineCacheManager(ArticleEnrichmentCategoryOptionProvider.CACHE_NAME);
    cacheManager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(properties.ttl()).maximumSize(1));
    return cacheManager;
  }
}

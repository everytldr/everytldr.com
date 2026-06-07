package com.everytldr.enricher;

import com.everytldr.enricher.enrichment.ArticleEnrichmentCategoryOptionProvider;
import com.everytldr.enricher.enrichment.EnricherCategoryOptionsCacheProperties;
import com.everytldr.enricher.enrichment.EnricherContentProperties;
import com.everytldr.enricher.enrichment.gemini.EnricherGeminiProperties;
import com.everytldr.enricher.processing.EnricherProcessingProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
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

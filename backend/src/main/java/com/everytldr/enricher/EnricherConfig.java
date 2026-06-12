package com.everytldr.enricher;

import com.everytldr.enricher.content.ArticleSourceProvider;
import com.everytldr.enricher.content.ContentCrawler;
import com.everytldr.enricher.content.ContentProperties;
import com.everytldr.enricher.content.CrawlingContentResolver;
import com.everytldr.enricher.enrichment.CategorySlugProvider;
import com.everytldr.enricher.enrichment.gemini.GeminiProperties;
import com.everytldr.enricher.processor.ProcessingProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
  ProcessingProperties.class,
  ContentProperties.class,
  EnricherConfig.EnricherCacheProperties.class,
  GeminiProperties.class
})
@EnableCaching
@EnableScheduling
@Profile("enricher")
public class EnricherConfig {
  @ConfigurationProperties(prefix = "everytldr.enricher.cache")
  public record EnricherCacheProperties(Duration categorySlugsTtl, Duration articleSourcesTtl) {

    public EnricherCacheProperties {
      if (categorySlugsTtl == null || categorySlugsTtl.isZero() || categorySlugsTtl.isNegative()) {
        throw new IllegalArgumentException("categorySlugsTtl must be positive");
      }
      if (articleSourcesTtl == null
          || articleSourcesTtl.isZero()
          || articleSourcesTtl.isNegative()) {
        throw new IllegalArgumentException("articleSourcesTtl must be positive");
      }
    }
  }

  @Bean
  ContentCrawler contentCrawler(ContentProperties contentProperties) {
    return new ContentCrawler(contentProperties.timeout(), contentProperties.maxBodyBytes());
  }

  @Bean
  CrawlingContentResolver crawlingContentResolver(
      ArticleSourceProvider articleSourceProvider,
      ContentCrawler contentCrawler,
      ContentProperties contentProperties) {
    return new CrawlingContentResolver(
        articleSourceProvider, contentCrawler, contentProperties.minBodyChars());
  }

  @Bean
  CacheManager enricherCacheManager(EnricherCacheProperties cacheProperties) {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.registerCustomCache(
        CategorySlugProvider.CACHE_NAME,
        Caffeine.newBuilder()
            .expireAfterWrite(cacheProperties.categorySlugsTtl())
            .maximumSize(1)
            .build());
    cacheManager.registerCustomCache(
        ArticleSourceProvider.CACHE_NAME,
        Caffeine.newBuilder()
            .expireAfterWrite(cacheProperties.articleSourcesTtl())
            .maximumSize(100)
            .build());
    return cacheManager;
  }
}

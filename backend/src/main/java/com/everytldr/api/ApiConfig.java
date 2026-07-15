package com.everytldr.api;

import com.everytldr.api.article.ArticlePopularityProperties;
import com.everytldr.api.article.ArticleViewMemoryGuardProperties;
import com.everytldr.api.article.ArticleViewProperties;
import com.everytldr.api.support.visitor.AnonymousVisitorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties({
  ArticleViewProperties.class,
  ArticleViewMemoryGuardProperties.class,
  ArticlePopularityProperties.class,
  AnonymousVisitorProperties.class
})
@EnableScheduling
@Profile("api")
public class ApiConfig {}

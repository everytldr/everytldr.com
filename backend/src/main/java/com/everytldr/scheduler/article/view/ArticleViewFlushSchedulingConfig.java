package com.everytldr.scheduler.article.view;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties({
  ArticleViewFlushProperties.class,
  ArticleViewFlushHistoryCleanupProperties.class
})
@EnableScheduling
@Profile("scheduler")
public class ArticleViewFlushSchedulingConfig {
  @Bean
  public ThreadPoolTaskScheduler articleViewFlushTaskScheduler() {
    return createTaskScheduler("article-view-flush-");
  }

  @Bean
  public ThreadPoolTaskScheduler articleViewFlushHistoryCleanupTaskScheduler() {
    return createTaskScheduler("article-view-flush-history-cleanup-");
  }

  private static ThreadPoolTaskScheduler createTaskScheduler(String threadNamePrefix) {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(1);
    taskScheduler.setThreadNamePrefix(threadNamePrefix);
    taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
    taskScheduler.setAwaitTerminationSeconds(5);
    return taskScheduler;
  }
}

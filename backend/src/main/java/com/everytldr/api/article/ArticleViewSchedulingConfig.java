package com.everytldr.api.article;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Profile("api")
public class ArticleViewSchedulingConfig {
  @Bean
  public ThreadPoolTaskScheduler articleViewMemoryTaskScheduler() {
    return createTaskScheduler("article-view-memory-");
  }

  @Bean
  public ThreadPoolTaskScheduler articleViewFlushTaskScheduler() {
    return createTaskScheduler("article-view-flush-");
  }

  @Bean
  public ThreadPoolTaskScheduler articleViewFlushHistoryCleanupTaskScheduler() {
    return createTaskScheduler("article-view-history-cleanup-");
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

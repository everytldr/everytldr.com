package com.everytldr.api.article.view;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Profile("api")
public class ArticleViewMemorySchedulingConfig {
  @Bean
  public ThreadPoolTaskScheduler articleViewMemoryTaskScheduler() {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(1);
    taskScheduler.setThreadNamePrefix("article-view-memory-");
    taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
    taskScheduler.setAwaitTerminationSeconds(5);
    return taskScheduler;
  }
}

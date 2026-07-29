package com.everytldr.scheduler.briefing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties({BriefingGenerationProperties.class, BriefingGeminiProperties.class})
@EnableScheduling
@Profile("scheduler")
public class BriefingSchedulingConfig {
  @Bean
  public ThreadPoolTaskScheduler briefingGenerationTaskScheduler() {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(1);
    taskScheduler.setThreadNamePrefix("briefing-generation-");
    taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
    taskScheduler.setAwaitTerminationSeconds(5);
    taskScheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    return taskScheduler;
  }
}

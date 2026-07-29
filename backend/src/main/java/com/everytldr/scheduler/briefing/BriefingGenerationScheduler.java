package com.everytldr.scheduler.briefing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("scheduler")
@ConditionalOnProperty(name = "everytldr.briefing.generation.enabled", havingValue = "true")
@Slf4j
public class BriefingGenerationScheduler {
  private final BriefingGenerationService generationService;

  @Scheduled(
      scheduler = "briefingGenerationTaskScheduler",
      cron = "${everytldr.briefing.generation.cron}",
      zone = "UTC")
  void generateDailyBriefing() {
    try {
      generationService.generateDailyBriefing();
    } catch (RuntimeException e) {
      log.warn("Failed to run briefing generation", e);
    }
  }
}

package com.everytldr.scheduler.article.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("scheduler")
@Slf4j
public class ArticleViewFlushHistoryCleanupScheduler {
  private final ArticleViewFlushHistoryCleanupService cleanupService;

  @Scheduled(
      scheduler = "articleViewFlushHistoryCleanupTaskScheduler",
      fixedDelayString = "${everytldr.article-view.flush.history-cleanup.interval}")
  void deleteExpiredHistory() {
    try {
      cleanupService.deleteExpiredHistory();
    } catch (RuntimeException e) {
      log.warn("Failed to clean up article view flush history", e);
    }
  }
}

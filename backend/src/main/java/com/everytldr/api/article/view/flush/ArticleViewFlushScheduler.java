package com.everytldr.api.article.view.flush;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("api")
@ConditionalOnProperty(
    name = "everytldr.article-view.flush-enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class ArticleViewFlushScheduler {
  private final ArticleViewFlushService flushService;

  @Scheduled(
      scheduler = "articleViewFlushTaskScheduler",
      cron = "${everytldr.article-view.flush-cron}",
      zone = "Asia/Seoul")
  void flushPendingViews() {
    try {
      flushService.flushPendingViews();
    } catch (RuntimeException e) {
      log.warn("Failed to run article view flush", e);
    }
  }
}

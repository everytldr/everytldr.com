package com.everytldr.scheduler.article.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class ArticleViewFlushSchedulingConfigTest {
  private final ArticleViewFlushSchedulingConfig config = new ArticleViewFlushSchedulingConfig();

  @Test
  void createsDedicatedSingleThreadSchedulers() {
    assertThat(config.articleViewFlushTaskScheduler().getPoolSize()).isEqualTo(1);
    assertThat(config.articleViewFlushHistoryCleanupTaskScheduler().getPoolSize()).isEqualTo(1);
  }

  @Test
  void assignsEachScheduledTaskToItsDedicatedScheduler() throws NoSuchMethodException {
    assertThat(findScheduled(ArticleViewFlushScheduler.class, "flushPendingViews").scheduler())
        .isEqualTo("articleViewFlushTaskScheduler");
    assertThat(
            findScheduled(ArticleViewFlushHistoryCleanupScheduler.class, "deleteExpiredHistory")
                .scheduler())
        .isEqualTo("articleViewFlushHistoryCleanupTaskScheduler");
  }

  private static Scheduled findScheduled(Class<?> type, String methodName)
      throws NoSuchMethodException {
    Method method = type.getDeclaredMethod(methodName);
    return method.getAnnotation(Scheduled.class);
  }
}

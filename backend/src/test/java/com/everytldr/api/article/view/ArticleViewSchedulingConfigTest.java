package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.api.article.view.flush.ArticleViewFlushHistoryCleanupScheduler;
import com.everytldr.api.article.view.flush.ArticleViewFlushScheduler;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class ArticleViewSchedulingConfigTest {
  private final ArticleViewSchedulingConfig config = new ArticleViewSchedulingConfig();

  @Test
  void createsDedicatedSingleThreadSchedulers() {
    assertThat(config.articleViewMemoryTaskScheduler().getPoolSize()).isEqualTo(1);
    assertThat(config.articleViewFlushTaskScheduler().getPoolSize()).isEqualTo(1);
    assertThat(config.articleViewFlushHistoryCleanupTaskScheduler().getPoolSize()).isEqualTo(1);
  }

  @Test
  void assignsEachScheduledTaskToItsDedicatedScheduler() throws NoSuchMethodException {
    assertThat(findScheduled(ArticleViewRedisMemoryGuard.class, "refreshMemoryUsage").scheduler())
        .isEqualTo("articleViewMemoryTaskScheduler");
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

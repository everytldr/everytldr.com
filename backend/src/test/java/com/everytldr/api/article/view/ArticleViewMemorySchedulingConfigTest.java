package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class ArticleViewMemorySchedulingConfigTest {
  private final ArticleViewMemorySchedulingConfig memoryConfig =
      new ArticleViewMemorySchedulingConfig();

  @Test
  void createsDedicatedSingleThreadScheduler() {
    assertThat(memoryConfig.articleViewMemoryTaskScheduler().getPoolSize()).isEqualTo(1);
  }

  @Test
  void cancelsDelayedMemorySamplingOnShutdown() {
    assertCancelsDelayedTasksOnShutdown(memoryConfig.articleViewMemoryTaskScheduler());
  }

  @Test
  void assignsMemorySamplingToItsDedicatedScheduler() throws NoSuchMethodException {
    assertThat(findScheduled(ArticleViewRedisMemoryGuard.class, "refreshMemoryUsage").scheduler())
        .isEqualTo("articleViewMemoryTaskScheduler");
  }

  private static Scheduled findScheduled(Class<?> type, String methodName)
      throws NoSuchMethodException {
    Method method = type.getDeclaredMethod(methodName);
    return method.getAnnotation(Scheduled.class);
  }

  private static void assertCancelsDelayedTasksOnShutdown(ThreadPoolTaskScheduler taskScheduler) {
    taskScheduler.initialize();
    try {
      assertThat(
              taskScheduler
                  .getScheduledThreadPoolExecutor()
                  .getExecuteExistingDelayedTasksAfterShutdownPolicy())
          .isFalse();
    } finally {
      taskScheduler.shutdown();
    }
  }
}

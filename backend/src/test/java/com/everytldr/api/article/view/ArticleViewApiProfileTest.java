package com.everytldr.api.article.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.scheduler.article.view.ArticleViewFlushHistoryCleanupScheduler;
import com.everytldr.scheduler.article.view.ArticleViewFlushScheduler;
import com.everytldr.scheduler.article.view.ArticleViewFlushService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"api", "test"})
class ArticleViewApiProfileTest {
  @Autowired private ApplicationContext applicationContext;

  @Test
  void createsOnlyApiArticleViewComponents() {
    assertThat(applicationContext.getBeansOfType(ArticleViewRedisMemoryGuard.class)).hasSize(1);
    assertThat(applicationContext.getBeansOfType(ArticleViewFlushService.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ArticleViewFlushScheduler.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ArticleViewFlushHistoryCleanupScheduler.class))
        .isEmpty();
  }
}

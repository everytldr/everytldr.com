package com.everytldr.scheduler.article.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.RedisTestcontainersConfig;
import com.everytldr.TestcontainersConfig;
import com.everytldr.api.article.ArticleController;
import com.everytldr.api.article.view.ArticleViewRedisMemoryGuard;
import com.everytldr.api.article.view.ArticleViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"scheduler", "test"})
@TestPropertySource(properties = "everytldr.article-view.flush.enabled=true")
class ArticleViewSchedulerProfileTest {
  @Autowired private ApplicationContext applicationContext;

  @Test
  void createsOnlySchedulerArticleViewComponents() {
    assertThat(applicationContext.getBeansOfType(ArticleViewFlushService.class)).hasSize(1);
    assertThat(applicationContext.getBeansOfType(ArticleViewFlushScheduler.class)).hasSize(1);
    assertThat(applicationContext.getBeansOfType(ArticleViewFlushHistoryCleanupScheduler.class))
        .hasSize(1);
    assertThat(applicationContext.getBeansOfType(ArticleController.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ArticleViewService.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ArticleViewRedisMemoryGuard.class)).isEmpty();
  }
}

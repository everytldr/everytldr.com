package com.everytldr;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.scheduler.article.view.ArticleViewFlushService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@ActiveProfiles({"monolith", "test"})
class ArticleViewMonolithProfileTest {
  @Autowired private Environment environment;
  @Autowired private ArticleViewFlushService flushService;

  @Test
  void activatesSchedulerWithMonolithProfile() {
    assertThat(environment.acceptsProfiles(Profiles.of("api"))).isTrue();
    assertThat(environment.acceptsProfiles(Profiles.of("scheduler"))).isTrue();
    assertThat(environment.acceptsProfiles(Profiles.of("ingestor"))).isTrue();
    assertThat(environment.acceptsProfiles(Profiles.of("enricher"))).isTrue();
    assertThat(flushService).isNotNull();
  }
}

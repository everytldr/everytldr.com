package com.everytldr.enricher.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ExtendWith(MockitoExtension.class)
class ArticleEnrichmentPollingSchedulerTest {

  @Mock private ArticleEnrichmentJobProcessor articleEnrichmentJobProcessor;

  @Test
  void runsProcessorWithConfiguredBatchSizeWhenEnabled() {
    ArticleEnrichmentPollingScheduler scheduler = scheduler(true, 3);
    when(articleEnrichmentJobProcessor.processNextJobs(3))
        .thenReturn(
            List.of(
                new ArticleEnrichmentProcessingResult(
                    100L, ArticleEnrichmentProcessingStatus.SUCCEEDED),
                new ArticleEnrichmentProcessingResult(
                    101L, ArticleEnrichmentProcessingStatus.RETRY_SCHEDULED)));

    scheduler.runArticleEnrichmentPolling();

    verify(articleEnrichmentJobProcessor).processNextJobs(3);
  }

  @Test
  void doesNotPropagateProcessorFailure() {
    ArticleEnrichmentPollingScheduler scheduler = scheduler(true, 5);
    when(articleEnrichmentJobProcessor.processNextJobs(5))
        .thenThrow(new IllegalStateException("processor failure"));

    assertThatCode(scheduler::runArticleEnrichmentPolling).doesNotThrowAnyException();
  }

  private ArticleEnrichmentPollingScheduler scheduler(boolean enabled, int batchSize) {
    return new ArticleEnrichmentPollingScheduler(
        articleEnrichmentJobProcessor,
        new EnricherProcessingProperties(
            enabled,
            batchSize,
            Duration.ofSeconds(30),
            3,
            Duration.ofMinutes(10),
            Duration.ofMinutes(15)));
  }

  @Nested
  class ConditionalRegistrationTest {
    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("enricher"))
            .withUserConfiguration(SchedulerContextConfig.class)
            .withPropertyValues(
                "everytldr.enricher.processing.batch-size=3",
                "everytldr.enricher.processing.fixed-delay=30s",
                "everytldr.enricher.processing.max-attempts=3",
                "everytldr.enricher.processing.retry-delay=10m",
                "everytldr.enricher.processing.stale-timeout=15m");

    @Test
    void registersSchedulerWhenEnabled() {
      contextRunner
          .withPropertyValues("everytldr.enricher.processing.enabled=true")
          .run(
              context ->
                  assertThat(context).hasSingleBean(ArticleEnrichmentPollingScheduler.class));
    }

    @Test
    void doesNotRegisterSchedulerWhenDisabled() {
      contextRunner
          .withPropertyValues("everytldr.enricher.processing.enabled=false")
          .run(
              context ->
                  assertThat(context).doesNotHaveBean(ArticleEnrichmentPollingScheduler.class));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(EnricherProcessingProperties.class)
  @Import(ArticleEnrichmentPollingScheduler.class)
  static class SchedulerContextConfig {
    @Bean
    ArticleEnrichmentJobProcessor articleEnrichmentJobProcessor() {
      return org.mockito.Mockito.mock(ArticleEnrichmentJobProcessor.class);
    }
  }
}

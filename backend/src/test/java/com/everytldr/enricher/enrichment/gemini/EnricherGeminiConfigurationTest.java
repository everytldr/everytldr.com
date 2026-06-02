package com.everytldr.enricher.enrichment.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.enricher.EnricherConfig;
import com.everytldr.enricher.enrichment.ArticleEnrichmentClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class EnricherGeminiConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
          .withInitializer(context -> context.getEnvironment().setActiveProfiles("enricher"))
          .withUserConfiguration(EnricherConfig.class, GeminiArticleEnrichmentClient.class)
          .withBean(RestClient.Builder.class, RestClient::builder)
          .withPropertyValues(commonProperties());

  @Test
  void doesNotRegisterGeminiClientWhenDisabled() {
    contextRunner
        .withPropertyValues("everytldr.enricher.ai.gemini.enabled=false")
        .run(
            context -> {
              assertThat(context).hasSingleBean(EnricherGeminiProperties.class);
              assertThat(context).doesNotHaveBean(ArticleEnrichmentClient.class);
              assertThat(context).doesNotHaveBean(GeminiArticleEnrichmentClient.class);
            });
  }

  @Test
  void registersGeminiClientWhenEnabled() {
    contextRunner
        .withPropertyValues(enabledGeminiProperties())
        .run(
            context -> {
              assertThat(context).hasSingleBean(GeminiArticleEnrichmentClient.class);
              assertThat(context).hasSingleBean(ArticleEnrichmentClient.class);
              assertThat(context).hasSingleBean(ObjectMapper.class);
              assertThat(context.getBean(EnricherGeminiProperties.class).model())
                  .isEqualTo("gemini-3.1-flash-lite");
            });
  }

  @Test
  void failsStartupWhenEnabledApiKeyIsBlank() {
    contextRunner
        .withPropertyValues(
            "everytldr.enricher.ai.gemini.enabled=true",
            "everytldr.enricher.ai.gemini.base-url=http://localhost",
            "everytldr.enricher.ai.gemini.api-key=",
            "everytldr.enricher.ai.gemini.model=gemini-3.1-flash-lite",
            "everytldr.enricher.ai.gemini.request-timeout=30s",
            "everytldr.enricher.ai.gemini.prompt-resource=classpath:prompts/article-enrichment-system-prompt.txt")
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .rootCause()
                    .hasMessageContaining("apiKey must not be blank"));
  }

  private String[] commonProperties() {
    return new String[] {
      "everytldr.enricher.processing.enabled=false",
      "everytldr.enricher.processing.batch-size=10",
      "everytldr.enricher.processing.fixed-delay=30s",
      "everytldr.enricher.processing.max-attempts=3",
      "everytldr.enricher.processing.retry-delay=10m",
      "everytldr.enricher.processing.stale-timeout=15m",
      "everytldr.enricher.content.allowed-hosts=localhost,globalvoices.org",
      "everytldr.enricher.content.request-timeout=5s",
      "everytldr.enricher.content.max-redirects=3",
      "everytldr.enricher.content.max-body-bytes=1048576",
      "everytldr.enricher.content.min-body-chars=200",
      "everytldr.enricher.cache.category-options.ttl=5m"
    };
  }

  private String[] enabledGeminiProperties() {
    return new String[] {
      "everytldr.enricher.ai.gemini.enabled=true",
      "everytldr.enricher.ai.gemini.base-url=http://localhost",
      "everytldr.enricher.ai.gemini.api-key=test-key",
      "everytldr.enricher.ai.gemini.model=gemini-3.1-flash-lite",
      "everytldr.enricher.ai.gemini.request-timeout=30s",
      "everytldr.enricher.ai.gemini.prompt-resource=classpath:prompts/article-enrichment-system-prompt.txt"
    };
  }
}

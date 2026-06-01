package org.everytldr.enricher.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import org.everytldr.enricher.EnricherConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EnricherContentConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withInitializer(context -> context.getEnvironment().setActiveProfiles("enricher"))
          .withUserConfiguration(EnricherConfig.class, ArticlePageContentResolver.class)
          .withPropertyValues(
              "everytldr.enricher.processing.enabled=false",
              "everytldr.enricher.processing.batch-size=10",
              "everytldr.enricher.processing.fixed-delay=30s",
              "everytldr.enricher.processing.max-attempts=3",
              "everytldr.enricher.processing.retry-delay=10m",
              "everytldr.enricher.processing.stale-timeout=15m",
              "everytldr.enricher.content.allowed-hosts=localhost,www.theguardian.com",
              "everytldr.enricher.content.request-timeout=5s",
              "everytldr.enricher.content.max-redirects=3",
              "everytldr.enricher.content.max-body-bytes=1048576",
              "everytldr.enricher.content.min-body-chars=200");

  @Test
  void registersContentResolverAndPropertiesInEnricherProfile() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(ArticlePageContentResolver.class);
          assertThat(context).hasSingleBean(EnricherContentProperties.class);
          assertThat(context).doesNotHaveBean(ArticleEnrichmentClient.class);
          assertThat(context.getBean(EnricherContentProperties.class).allowedHosts())
              .containsExactly("localhost", "www.theguardian.com");
        });
  }
}

package org.everytldr.enricher;

import org.everytldr.enricher.enrichment.EnricherContentProperties;
import org.everytldr.enricher.enrichment.gemini.EnricherGeminiProperties;
import org.everytldr.enricher.processing.EnricherProcessingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties({
  EnricherProcessingProperties.class,
  EnricherContentProperties.class,
  EnricherGeminiProperties.class
})
@EnableScheduling
@Profile("enricher")
public class EnricherConfig {}

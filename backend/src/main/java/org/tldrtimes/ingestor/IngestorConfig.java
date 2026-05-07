package org.tldrtimes.ingestor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.tldrtimes.ingestor.provider.guardian.GuardianArticleSourceClient;

@Configuration
@EnableConfigurationProperties(GuardianArticleSourceClient.ClientProperties.class)
@EnableScheduling
@Profile("ingestor")
public class IngestorConfig {}

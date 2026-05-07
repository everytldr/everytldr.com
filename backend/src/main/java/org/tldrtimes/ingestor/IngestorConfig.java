package org.tldrtimes.ingestor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.tldrtimes.ingestor.provider.guardian.GuardianArticleSourceClient;

@Configuration
@EnableConfigurationProperties(GuardianArticleSourceClient.ClientProperties.class)
@Profile("ingestor")
public class IngestorConfig {}

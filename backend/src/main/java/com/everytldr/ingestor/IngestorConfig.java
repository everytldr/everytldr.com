package com.everytldr.ingestor;

import com.everytldr.ingestor.provider.guardian.GuardianArticleSourceClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(GuardianArticleSourceClient.ClientProperties.class)
@EnableScheduling
@Profile("ingestor")
public class IngestorConfig {}

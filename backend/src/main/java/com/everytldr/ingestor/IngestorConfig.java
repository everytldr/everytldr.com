package com.everytldr.ingestor;

import com.everytldr.ingestor.source.rss.FeedProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(FeedProperties.class)
@Profile("ingestor")
public class IngestorConfig {}

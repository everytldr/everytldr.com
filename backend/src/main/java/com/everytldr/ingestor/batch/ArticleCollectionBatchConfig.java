package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.ingestion.IngestionExceptions;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import java.time.Duration;
import java.util.Objects;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ArticleCollectionBatchConfig.IngestorBatchProperties.class)
@Profile("ingestor")
public class ArticleCollectionBatchConfig {

  public static final String JOB_NAME = "articleCollectionJob";
  public static final String STEP_NAME = "articleCollectionStep";

  @Bean
  Job articleCollectionJob(JobRepository jobRepository, Step articleCollectionStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(articleCollectionStep).build();
  }

  @Bean
  Step articleCollectionStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ItemReader<ArticleCollectionTarget> itemReader,
      ItemProcessor<ArticleCollectionTarget, ArticleCollectionResult> itemProcessor,
      ItemWriter<ArticleCollectionResult> itemWriter,
      ArticleCollectionSkipListener articleCollectionSkipListener,
      ArticleCollectionStepListener articleCollectionStepListener,
      RetryPolicy articleCollectionRetryPolicy,
      IngestorBatchProperties properties) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ArticleCollectionTarget, ArticleCollectionResult>chunk(properties.chunkSize())
        .transactionManager(transactionManager)
        .reader(itemReader)
        .processor(itemProcessor)
        .writer(itemWriter)
        .faultTolerant()
        .retryPolicy(articleCollectionRetryPolicy)
        .skip(IngestionExceptions.Retryable.class)
        .skip(IngestionExceptions.Skippable.class)
        .skipLimit(properties.skipLimit())
        .listener(articleCollectionSkipListener)
        .listener(articleCollectionStepListener)
        .build();
  }

  @Bean
  RetryPolicy articleCollectionRetryPolicy(IngestorBatchProperties properties) {
    IngestorBatchProperties.RetryProperties retry = properties.retry();
    return RetryPolicy.builder()
        .includes(IngestionExceptions.Retryable.class)
        .maxRetries(retry.limit())
        .delay(retry.initialInterval())
        .multiplier(retry.multiplier())
        .maxDelay(retry.maxInterval())
        .build();
  }

  @ConfigurationProperties("everytldr.ingestor.batch")
  public record IngestorBatchProperties(int chunkSize, RetryProperties retry, int skipLimit) {
    public IngestorBatchProperties {
      Objects.requireNonNull(retry, "retry must not be null");
      if (chunkSize < 1) {
        throw new IllegalArgumentException("chunkSize must be positive");
      }
      if (skipLimit < 0) {
        throw new IllegalArgumentException("skipLimit must not be negative");
      }
    }

    public record RetryProperties(
        int limit, Duration initialInterval, double multiplier, Duration maxInterval) {
      public RetryProperties {
        Objects.requireNonNull(initialInterval, "initialInterval must not be null");
        Objects.requireNonNull(maxInterval, "maxInterval must not be null");
        if (limit < 0) {
          throw new IllegalArgumentException("retry.limit must not be negative");
        }
        if (initialInterval.toMillis() < 1) {
          throw new IllegalArgumentException("retry.initialInterval must be at least 1ms");
        }
        if (multiplier <= 1.0) {
          throw new IllegalArgumentException("retry.multiplier must be greater than 1.0");
        }
        if (maxInterval.toMillis() < 1) {
          throw new IllegalArgumentException("retry.maxInterval must be at least 1ms");
        }
        if (maxInterval.compareTo(initialInterval) < 0) {
          throw new IllegalArgumentException(
              "retry.maxInterval must not be shorter than retry.initialInterval");
        }
      }
    }
  }
}

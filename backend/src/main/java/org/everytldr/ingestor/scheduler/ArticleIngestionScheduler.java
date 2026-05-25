package org.everytldr.ingestor.scheduler;

import java.time.Clock;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.everytldr.ingestor.batch.ArticleIngestionBatchConfig;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("ingestor")
@ConditionalOnProperty(name = "everytldr.ingestor.ingestion.enabled", havingValue = "true")
@Slf4j
public class ArticleIngestionScheduler {

  private final JobOperator jobOperator;
  private final Job articleIngestionBatchJob;
  private final Clock clock;

  public ArticleIngestionScheduler(
      JobOperator jobOperator,
      @Qualifier(ArticleIngestionBatchConfig.JOB_NAME) Job articleIngestionBatchJob,
      Clock clock) {
    this.jobOperator = jobOperator;
    this.articleIngestionBatchJob = articleIngestionBatchJob;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${everytldr.ingestor.ingestion.fixed-delay}")
  void runArticleIngestionJob() {
    Instant scheduledAt = Instant.now(clock);
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("scheduledAt", scheduledAt.toString())
            .toJobParameters();

    try {
      JobExecution jobExecution = jobOperator.start(articleIngestionBatchJob, jobParameters);
      log.info(
          "Started article ingestion batch job. jobExecutionId={}, scheduledAt={}",
          jobExecution.getId(),
          scheduledAt);
    } catch (Exception e) {
      log.warn("Failed to start article ingestion batch job. scheduledAt={}", scheduledAt, e);
    }
  }
}

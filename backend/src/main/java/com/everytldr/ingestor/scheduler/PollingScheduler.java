package com.everytldr.ingestor.scheduler;

import com.everytldr.ingestor.batch.ArticleCollectionBatchConfig;
import com.everytldr.ingestor.ingestion.IngestionMetrics;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("ingestorPollingScheduler")
@Profile("ingestor")
@ConditionalOnProperty(name = "everytldr.ingestor.ingestion.enabled", havingValue = "true")
@Slf4j
public class PollingScheduler {

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;
  private final Job articleCollectionJob;
  private final IngestionMetrics ingestionMetrics;
  private final Clock clock;

  public PollingScheduler(
      JobOperator jobOperator,
      JobRepository jobRepository,
      @Qualifier(ArticleCollectionBatchConfig.JOB_NAME) Job articleCollectionJob,
      IngestionMetrics ingestionMetrics,
      Clock clock) {
    this.jobOperator = jobOperator;
    this.jobRepository = jobRepository;
    this.articleCollectionJob = articleCollectionJob;
    this.ingestionMetrics = ingestionMetrics;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${everytldr.ingestor.ingestion.fixed-delay}")
  void runArticleCollectionJob() {
    Instant scheduledAt = Instant.now(clock);
    try {
      Set<JobExecution> runningJobExecutions =
          jobRepository.findRunningJobExecutions(ArticleCollectionBatchConfig.JOB_NAME);
      if (!runningJobExecutions.isEmpty()) {
        ingestionMetrics.recordArticleCollectionJobStart("already_running");
        log.info(
            "Skipped article collection job because another execution is running. runningExecutions={}, scheduledAt={}",
            runningJobExecutions.size(),
            scheduledAt);
        return;
      }

      log.info("Launching article collection job. scheduledAt={}", scheduledAt);

      JobParameters jobParameters =
          new JobParametersBuilder()
              .addString("scheduledAt", scheduledAt.toString())
              .toJobParameters();
      JobExecution jobExecution = jobOperator.start(articleCollectionJob, jobParameters);
      ingestionMetrics.recordArticleCollectionJobStart("started");

      log.info(
          "Completed article collection job launch. jobExecutionId={}, scheduledAt={}",
          jobExecution.getId(),
          scheduledAt);

    } catch (Exception e) {
      ingestionMetrics.recordArticleCollectionJobStart("failed");
      log.warn("Failed to launch article collection job. scheduledAt={}", scheduledAt, e);
    }
  }
}

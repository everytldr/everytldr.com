package com.everytldr.ingestor.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.ingestor.batch.ArticleCollectionBatchConfig;
import com.everytldr.ingestor.ingestion.IngestionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

class PollingSchedulerTest {

  @Test
  void startsBatchJobWithScheduledAtParameter() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    JobRepository jobRepository = mock(JobRepository.class);
    Job ingestionBatchJob = mock(Job.class);
    JobExecution jobExecution = mock(JobExecution.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    Clock clock = Clock.fixed(Instant.parse("2026-05-08T01:02:03Z"), ZoneOffset.UTC);
    PollingScheduler scheduler =
        new PollingScheduler(
            jobOperator,
            jobRepository,
            ingestionBatchJob,
            new IngestionMetrics(meterRegistry),
            clock);

    when(jobRepository.findRunningJobExecutions(ArticleCollectionBatchConfig.JOB_NAME))
        .thenReturn(Set.of());
    when(jobOperator.start(same(ingestionBatchJob), any())).thenReturn(jobExecution);

    scheduler.runArticleCollectionJob();

    ArgumentCaptor<JobParameters> jobParametersCaptor =
        ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator).start(same(ingestionBatchJob), jobParametersCaptor.capture());
    verify(jobOperator, never()).restart(org.mockito.ArgumentMatchers.any(JobExecution.class));
    assertThat(jobParametersCaptor.getValue().getString("scheduledAt"))
        .isEqualTo("2026-05-08T01:02:03Z");
    assertThat(jobStartCount(meterRegistry, "started")).isEqualTo(1.0);
  }

  @Test
  void skipsBatchJobWhenAnotherExecutionIsRunning() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    JobRepository jobRepository = mock(JobRepository.class);
    Job ingestionBatchJob = mock(Job.class);
    JobExecution runningJobExecution = mock(JobExecution.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    Clock clock = Clock.fixed(Instant.parse("2026-05-08T01:02:03Z"), ZoneOffset.UTC);
    PollingScheduler scheduler =
        new PollingScheduler(
            jobOperator,
            jobRepository,
            ingestionBatchJob,
            new IngestionMetrics(meterRegistry),
            clock);

    when(jobRepository.findRunningJobExecutions(ArticleCollectionBatchConfig.JOB_NAME))
        .thenReturn(Set.of(runningJobExecution));

    scheduler.runArticleCollectionJob();

    verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    assertThat(jobStartCount(meterRegistry, "already_running")).isEqualTo(1.0);
  }

  @Test
  void doesNotPropagateBatchStartFailure() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    JobRepository jobRepository = mock(JobRepository.class);
    Job ingestionBatchJob = mock(Job.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    Clock clock = Clock.fixed(Instant.parse("2026-05-08T01:02:03Z"), ZoneOffset.UTC);
    PollingScheduler scheduler =
        new PollingScheduler(
            jobOperator,
            jobRepository,
            ingestionBatchJob,
            new IngestionMetrics(meterRegistry),
            clock);

    when(jobRepository.findRunningJobExecutions(ArticleCollectionBatchConfig.JOB_NAME))
        .thenReturn(Set.of());
    when(jobOperator.start(same(ingestionBatchJob), any()))
        .thenThrow(new IllegalStateException("batch start failed"));

    assertThatCode(scheduler::runArticleCollectionJob).doesNotThrowAnyException();
    assertThat(jobStartCount(meterRegistry, "failed")).isEqualTo(1.0);
  }

  private double jobStartCount(SimpleMeterRegistry meterRegistry, String outcome) {
    return meterRegistry
        .get("everytldr.ingestor.article_collection.job.starts")
        .tag("outcome", outcome)
        .counter()
        .count();
  }
}

package org.tldrtimes.ingestor.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

class ArticleIngestionSchedulerTest {

  @Test
  void startsArticleIngestionBatchJobWithScheduledAtParameter() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    Job articleIngestionBatchJob = mock(Job.class);
    JobExecution jobExecution = mock(JobExecution.class);
    Clock clock = Clock.fixed(Instant.parse("2026-05-08T01:02:03Z"), ZoneOffset.UTC);
    ArticleIngestionScheduler scheduler =
        new ArticleIngestionScheduler(jobOperator, articleIngestionBatchJob, clock);

    when(jobOperator.start(same(articleIngestionBatchJob), org.mockito.ArgumentMatchers.any()))
        .thenReturn(jobExecution);

    scheduler.runArticleIngestionJob();

    ArgumentCaptor<JobParameters> jobParametersCaptor =
        ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator).start(same(articleIngestionBatchJob), jobParametersCaptor.capture());
    assertThat(jobParametersCaptor.getValue().getString("scheduledAt"))
        .isEqualTo("2026-05-08T01:02:03Z");
  }

  @Test
  void doesNotPropagateBatchStartFailure() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    Job articleIngestionBatchJob = mock(Job.class);
    Clock clock = Clock.fixed(Instant.parse("2026-05-08T01:02:03Z"), ZoneOffset.UTC);
    ArticleIngestionScheduler scheduler =
        new ArticleIngestionScheduler(jobOperator, articleIngestionBatchJob, clock);

    when(jobOperator.start(same(articleIngestionBatchJob), org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("batch is already running"));

    assertThatCode(scheduler::runArticleIngestionJob).doesNotThrowAnyException();
  }
}

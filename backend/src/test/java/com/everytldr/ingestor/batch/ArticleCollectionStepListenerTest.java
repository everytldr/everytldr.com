package com.everytldr.ingestor.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.everytldr.ingestor.ingestion.IngestionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

class ArticleCollectionStepListenerTest {

  @Test
  void startsStepWithoutFailure() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionStepListener listener =
        new ArticleCollectionStepListener(new IngestionMetrics(meterRegistry));
    StepExecution stepExecution = stepExecution();

    assertThatCode(() -> listener.beforeStep(stepExecution)).doesNotThrowAnyException();
  }

  @Test
  void recordsCompletedStepCompletion() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionStepListener listener =
        new ArticleCollectionStepListener(new IngestionMetrics(meterRegistry));
    StepExecution stepExecution = stepExecution();
    stepExecution.setStatus(BatchStatus.COMPLETED);
    stepExecution.setExitStatus(ExitStatus.COMPLETED);
    stepExecution.setReadCount(1);

    ExitStatus exitStatus = listener.afterStep(stepExecution);

    assertThat(exitStatus).isEqualTo(ExitStatus.COMPLETED);
    assertThat(stepCompletionCount(meterRegistry, "completed", "completed")).isEqualTo(1.0);
  }

  @Test
  void recordsFailedStepCompletion() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionStepListener listener =
        new ArticleCollectionStepListener(new IngestionMetrics(meterRegistry));
    StepExecution stepExecution = stepExecution();
    stepExecution.setStatus(BatchStatus.FAILED);
    stepExecution.setExitStatus(ExitStatus.FAILED);
    stepExecution.setReadCount(1);

    ExitStatus exitStatus = listener.afterStep(stepExecution);

    assertThat(exitStatus).isEqualTo(ExitStatus.FAILED);
    assertThat(stepCompletionCount(meterRegistry, "failed", "failed")).isEqualTo(1.0);
  }

  @Test
  void failsAllSkippedStepAndRecordsFailedCompletion() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ArticleCollectionStepListener listener =
        new ArticleCollectionStepListener(new IngestionMetrics(meterRegistry));
    StepExecution stepExecution = stepExecution();
    stepExecution.setStatus(BatchStatus.COMPLETED);
    stepExecution.setExitStatus(ExitStatus.COMPLETED);
    stepExecution.setReadCount(2);
    stepExecution.setProcessSkipCount(2);
    stepExecution.getExecutionContext().putInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY, 2);

    ExitStatus exitStatus = listener.afterStep(stepExecution);

    assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(exitStatus.getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(exitStatus.getExitDescription())
        .contains("All article collection targets were skipped");
    assertThat(
            stepExecution
                .getExecutionContext()
                .getInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY))
        .isZero();
    assertThat(stepCompletionCount(meterRegistry, "failed", "failed")).isEqualTo(1.0);
  }

  private double stepCompletionCount(
      SimpleMeterRegistry meterRegistry, String status, String exitCode) {
    return meterRegistry
        .get("everytldr.ingestor.article_collection.step.completions")
        .tag("status", status)
        .tag("exit_code", exitCode)
        .counter()
        .count();
  }

  private StepExecution stepExecution() {
    JobExecution jobExecution =
        new JobExecution(
            1L, new JobInstance(1L, ArticleCollectionBatchConfig.JOB_NAME), new JobParameters());
    return new StepExecution(1L, "articleCollectionStep", jobExecution);
  }
}

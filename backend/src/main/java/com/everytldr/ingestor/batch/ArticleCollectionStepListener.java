package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.ingestion.IngestionMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleCollectionStepListener implements StepExecutionListener {

  private final IngestionMetrics ingestionMetrics;

  @Override
  public void beforeStep(@NonNull StepExecution stepExecution) {
    log.info(
        "Starting article collection step. jobExecutionId={}, stepExecutionId={}, stepName={}, scheduledAt={}",
        stepExecution.getJobExecution().getId(),
        stepExecution.getId(),
        stepExecution.getStepName(),
        stepExecution.getJobExecution().getJobParameters().getString("scheduledAt"));
  }

  @Override
  public @Nullable ExitStatus afterStep(@NonNull StepExecution stepExecution) {
    ExitStatus exitStatus = stepExecution.getExitStatus();
    if (hasSkippedAllReadTargets(stepExecution)) {
      ArticleCollectionTargetReader.resetSavedNextIndex(stepExecution.getExecutionContext());
      stepExecution.setStatus(BatchStatus.FAILED);
      exitStatus =
          ExitStatus.FAILED.addExitDescription("All article collection targets were skipped");
    }

    ingestionMetrics.recordArticleCollectionStepCompletion(
        stepExecution.getStatus().name(), exitStatus.getExitCode());
    log.info(
        "Finished article collection step. status={}, exitCode={}, targetReadCount={}, targetWriteCount={}, processSkipCount={}, writeSkipCount={}, rollbackCount={}",
        stepExecution.getStatus(),
        exitStatus.getExitCode(),
        stepExecution.getReadCount(),
        stepExecution.getWriteCount(),
        stepExecution.getProcessSkipCount(),
        stepExecution.getWriteSkipCount(),
        stepExecution.getRollbackCount());
    return exitStatus;
  }

  private boolean hasSkippedAllReadTargets(StepExecution stepExecution) {
    long readCount = stepExecution.getReadCount();
    return readCount > 0 && stepExecution.getProcessSkipCount() == readCount;
  }
}

package com.everytldr.ingestor.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.everytldr.TestcontainersConfig;
import com.everytldr.ingestor.ingestion.IngestionService;
import com.everytldr.ingestor.ingestion.IngestionService.IngestionSummary;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles({"test", "ingestor"})
@TestPropertySource(properties = "spring.batch.job.enabled=false")
class BatchConfigTest {

  @Autowired private JobOperator jobOperator;

  @Autowired
  @Qualifier(BatchConfig.JOB_NAME)
  private Job ingestionBatchJob;

  @MockitoBean private IngestionService ingestionService;

  @Test
  void ingestionBatchJobRunsIngestionService() throws Exception {
    doReturn(new IngestionSummary(1, 0)).when(ingestionService).ingestActiveSources();

    JobExecution jobExecution =
        jobOperator.start(
            ingestionBatchJob,
            new JobParametersBuilder().addLong("run.id", System.nanoTime()).toJobParameters());

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ingestionService, atLeastOnce()).ingestActiveSources();
  }
}

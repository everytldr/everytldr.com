package com.everytldr.ingestor.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.everytldr.TestcontainersConfig;
import com.everytldr.ingestor.ingestion.ArticleIngestionService;
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
class ArticleIngestionBatchConfigTest {

  @Autowired private JobOperator jobOperator;

  @Autowired
  @Qualifier(ArticleIngestionBatchConfig.JOB_NAME)
  private Job articleIngestionBatchJob;

  @MockitoBean private ArticleIngestionService articleIngestionService;

  @Test
  void articleIngestionBatchJobRunsIngestionService() throws Exception {
    JobExecution jobExecution =
        jobOperator.start(
            articleIngestionBatchJob,
            new JobParametersBuilder().addLong("run.id", System.nanoTime()).toJobParameters());

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(articleIngestionService).ingestActiveSources();
  }
}

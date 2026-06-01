package com.everytldr.ingestor.batch;

import com.everytldr.ingestor.ingestion.ArticleIngestionService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("ingestor")
public class ArticleIngestionBatchConfig {

  public static final String JOB_NAME = "articleIngestionBatchJob";
  public static final String STEP_NAME = "ingestActiveSourcesStep";

  @Bean
  Job articleIngestionBatchJob(JobRepository jobRepository, Step ingestActiveSourcesStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(ingestActiveSourcesStep).build();
  }

  @Bean
  Step ingestActiveSourcesStep(
      JobRepository jobRepository, ArticleIngestionService articleIngestionService) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              articleIngestionService.ingestActiveSources();
              return RepeatStatus.FINISHED;
            })
        .build();
  }
}

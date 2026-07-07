package com.everytldr.ingestor.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import com.everytldr.ingestor.ingestion.CollectedArticleSaveService;
import com.everytldr.ingestor.ingestion.IngestionExceptions;
import com.everytldr.ingestor.source.ArticleCollectionTarget;
import com.everytldr.ingestor.source.CollectedArticle;
import com.everytldr.ingestor.source.SourceClient;
import com.everytldr.ingestor.source.SourceClientRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
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
@TestPropertySource(
    properties = {
      "spring.batch.job.enabled=false",
      "everytldr.ingestor.batch.chunk-size=2",
      "everytldr.ingestor.batch.retry.limit=1",
      "everytldr.ingestor.batch.retry.initial-interval=1ms",
      "everytldr.ingestor.batch.retry.max-interval=1ms",
      "everytldr.ingestor.batch.skip-limit=1"
    })
class BatchConfigTest {

  private static final String SUCCESS_FEED_URL = "https://feeds.example.com/success.xml";
  private static final String FAILED_FEED_URL = "https://feeds.example.com/failed.xml";
  private static final String ALSO_FAILED_FEED_URL = "https://feeds.example.com/also-failed.xml";
  private static final Instant PUBLISHED_AT = Instant.parse("2026-05-08T08:25:43Z");

  @Autowired private JobOperator jobOperator;

  @Autowired
  @Qualifier(ArticleCollectionBatchConfig.JOB_NAME)
  private Job articleCollectionJob;

  @MockitoBean private ArticleSourceRepository articleSourceRepository;

  @MockitoBean private SourceClientRegistry sourceClientRegistry;

  @MockitoBean private CollectedArticleSaveService collectedArticleSaveService;

  @Test
  void articleCollectionJobSavesCollectedArticles() throws Exception {
    ArticleSource source = sourceWithFeeds(SUCCESS_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    CollectedArticle collectedArticle = collectedArticle("https://news.example.com/success");
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(new ArticleCollectionTarget(source, SUCCESS_FEED_URL)))
        .thenReturn(List.of(collectedArticle));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
  }

  @Test
  void articleCollectionJobCompletesWhenSomeTargetsAreSkipped() throws Exception {
    ArticleSource source = sourceWithFeeds(SUCCESS_FEED_URL, FAILED_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    CollectedArticle collectedArticle = collectedArticle("https://news.example.com/success");
    ArticleCollectionTarget successTarget = new ArticleCollectionTarget(source, SUCCESS_FEED_URL);
    ArticleCollectionTarget failedTarget = new ArticleCollectionTarget(source, FAILED_FEED_URL);
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(successTarget)).thenReturn(List.of(collectedArticle));
    when(sourceClient.collect(failedTarget))
        .thenThrow(
            new IngestionExceptions.Retryable(
                "temporary RSS failure", new RuntimeException("boom")));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(getStepExecution(jobExecution).getProcessSkipCount()).isEqualTo(1);
    verify(sourceClient, times(2)).collect(failedTarget);
    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
  }

  @Test
  void articleCollectionJobCompletesWhenRetryableTargetRecovers() throws Exception {
    ArticleSource source = sourceWithFeeds(FAILED_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    CollectedArticle collectedArticle = collectedArticle("https://news.example.com/recovered");
    ArticleCollectionTarget target = new ArticleCollectionTarget(source, FAILED_FEED_URL);
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(target))
        .thenThrow(
            new IngestionExceptions.Retryable(
                "temporary RSS failure", new RuntimeException("boom")))
        .thenReturn(List.of(collectedArticle));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(getStepExecution(jobExecution).getProcessSkipCount()).isZero();
    verify(sourceClient, times(2)).collect(target);
    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
  }

  @Test
  void articleCollectionJobFailsWhenAllTargetsAreSkipped() throws Exception {
    ArticleSource source = sourceWithFeeds(FAILED_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(new ArticleCollectionTarget(source, FAILED_FEED_URL)))
        .thenThrow(
            new IngestionExceptions.Skippable("invalid RSS feed", new RuntimeException("boom")));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(jobExecution.getExitStatus().getExitCode())
        .isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(getStepExecution(jobExecution).getProcessSkipCount()).isEqualTo(1);
    verify(collectedArticleSaveService, never()).saveNewArticles(any());
  }

  @Test
  void articleCollectionJobRestartsAllSkippedTargetsFromBeginning() throws Exception {
    ArticleSource source = sourceWithFeeds(FAILED_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    CollectedArticle collectedArticle = collectedArticle("https://news.example.com/recovered");
    ArticleCollectionTarget target = new ArticleCollectionTarget(source, FAILED_FEED_URL);
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(target))
        .thenThrow(
            new IngestionExceptions.Skippable("invalid RSS feed", new RuntimeException("boom")))
        .thenReturn(List.of(collectedArticle));

    JobExecution firstExecution = startJob();

    assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(
            getStepExecution(firstExecution)
                .getExecutionContext()
                .getInt(ArticleCollectionTargetReader.NEXT_INDEX_KEY))
        .isZero();

    JobExecution restartedExecution = jobOperator.restart(firstExecution);

    assertThat(restartedExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    verify(sourceClient, times(2)).collect(target);
    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
  }

  @Test
  void articleCollectionJobFailsWhenSkipLimitIsExceeded() throws Exception {
    ArticleSource source = sourceWithFeeds(FAILED_FEED_URL, ALSO_FAILED_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(new ArticleCollectionTarget(source, FAILED_FEED_URL)))
        .thenThrow(
            new IngestionExceptions.Skippable("invalid RSS feed", new RuntimeException("boom")));
    when(sourceClient.collect(new ArticleCollectionTarget(source, ALSO_FAILED_FEED_URL)))
        .thenThrow(
            new IngestionExceptions.Skippable("invalid RSS feed", new RuntimeException("boom")));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(jobExecution.getExitStatus().getExitCode())
        .isEqualTo(ExitStatus.FAILED.getExitCode());
    verify(collectedArticleSaveService, never()).saveNewArticles(any());
  }

  @Test
  void articleCollectionJobFailsWhenWriterFails() throws Exception {
    ArticleSource source = sourceWithFeeds(SUCCESS_FEED_URL);
    SourceClient sourceClient = mock(SourceClient.class);
    CollectedArticle collectedArticle = collectedArticle("https://news.example.com/success");
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS)).thenReturn(sourceClient);
    when(sourceClient.collect(new ArticleCollectionTarget(source, SUCCESS_FEED_URL)))
        .thenReturn(List.of(collectedArticle));
    doThrow(new IllegalStateException("database is unavailable"))
        .when(collectedArticleSaveService)
        .saveNewArticles(List.of(collectedArticle));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(jobExecution.getExitStatus().getExitCode())
        .isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(getStepExecution(jobExecution).getWriteSkipCount()).isZero();
    verify(collectedArticleSaveService).saveNewArticles(List.of(collectedArticle));
  }

  @Test
  void articleCollectionJobFailsWhenActiveSourceFeedUrlIsInvalid() throws Exception {
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc())
        .thenThrow(
            new IllegalArgumentException("feedUrls must contain only absolute HTTP(S) URLs"));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(jobExecution.getExitStatus().getExitCode())
        .isEqualTo(ExitStatus.FAILED.getExitCode());
    verify(collectedArticleSaveService, never()).saveNewArticles(any());
  }

  @Test
  void articleCollectionJobFailsWhenFatalExceptionOccurs() throws Exception {
    ArticleSource source = sourceWithFeeds(FAILED_FEED_URL);
    when(articleSourceRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(source));
    when(sourceClientRegistry.getClient(SourceType.RSS))
        .thenThrow(new IllegalStateException("No SourceClient supports sourceType: RSS"));

    JobExecution jobExecution = startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(jobExecution.getExitStatus().getExitCode())
        .isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(getStepExecution(jobExecution).getProcessSkipCount()).isZero();
    verify(collectedArticleSaveService, never()).saveNewArticles(any());
  }

  private JobExecution startJob() throws Exception {
    return jobOperator.start(
        articleCollectionJob,
        new JobParametersBuilder()
            .addString("run.id", UUID.randomUUID().toString())
            .toJobParameters());
  }

  private StepExecution getStepExecution(JobExecution jobExecution) {
    return jobExecution.getStepExecutions().iterator().next();
  }

  private ArticleSource sourceWithFeeds(String... feedUrls) {
    return ArticleSource.create(
        "Example News",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of(feedUrls),
                List.of("news.example.com"),
                List.of("article"),
                List.of(),
                List.of())),
        "en",
        SourceType.RSS,
        LicenseInfo.createCcBy("4.0"));
  }

  private CollectedArticle collectedArticle(String contentUrl) {
    return new CollectedArticle(
        contentUrl, "Example News", null, "en", PUBLISHED_AT, LicenseInfo.createCcBy("4.0"));
  }
}

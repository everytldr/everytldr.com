CREATE INDEX `idx_article_ingestion_job_state_attempt_started_at`
    ON `article_ingestion_job` (`state`, `attempt_started_at`);

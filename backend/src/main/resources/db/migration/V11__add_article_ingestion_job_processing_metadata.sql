ALTER TABLE `article_ingestion_job`
    ADD COLUMN `attempt_count`      INT           NOT NULL DEFAULT 0 AFTER `state`,
    ADD COLUMN `attempt_started_at` DATETIME(6)   NULL AFTER `attempt_count`,
    ADD COLUMN `next_attempt_at`    DATETIME(6)   NULL AFTER `attempt_started_at`,
    ADD COLUMN `last_error_message` VARCHAR(1000) NULL AFTER `next_attempt_at`;

CREATE INDEX `idx_article_ingestion_job_state_next_attempt_at`
    ON `article_ingestion_job` (`state`, `next_attempt_at`);

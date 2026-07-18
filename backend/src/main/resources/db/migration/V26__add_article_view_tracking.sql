ALTER TABLE article
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE article_view_flush_history (
    batch_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id)
);

CREATE INDEX idx_article_view_flush_history_created_at
    ON article_view_flush_history (created_at);

RENAME TABLE `rss_source` TO `article_source`;

ALTER TABLE `article_source`
    RENAME INDEX `uk_rss_source_url` TO `uk_article_source_url`,
    RENAME INDEX `idx_rss_source_is_active` TO `idx_article_source_is_active`;

-- 기존 rss_source 데이터가 있을 수 있으므로 nullable 컬럼으로 먼저 추가하고,
-- 기존 row를 RSS로 보정한 뒤 NOT NULL 제약을 적용한다.
ALTER TABLE `article_source`
    ADD COLUMN `source_type` varchar(32) NULL AFTER `language`;

UPDATE `article_source`
SET `source_type` = 'RSS'
WHERE `source_type` IS NULL;

ALTER TABLE `article_source`
    MODIFY COLUMN `source_type` varchar(32) NOT NULL;
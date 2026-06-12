-- Pre-launch normalization: make article.source a foreign key to article_source.name
-- and rename source_url to content_url. Articles whose source no longer exists
-- (e.g. sources removed in V10) are orphans and are removed with their dependents
-- so the NOT NULL foreign key can be added.

DELETE FROM `article_category`
WHERE `article_id` IN (
    SELECT `id` FROM `article` WHERE `source` NOT IN (SELECT `name` FROM `article_source`));

DELETE FROM `article_like`
WHERE `article_id` IN (
    SELECT `id` FROM `article` WHERE `source` NOT IN (SELECT `name` FROM `article_source`));

DELETE FROM `article_comment`
WHERE `parent_id` IS NOT NULL
  AND `article_id` IN (
    SELECT `id` FROM `article` WHERE `source` NOT IN (SELECT `name` FROM `article_source`));

DELETE FROM `article_comment`
WHERE `article_id` IN (
    SELECT `id` FROM `article` WHERE `source` NOT IN (SELECT `name` FROM `article_source`));

DELETE FROM `article_summary`
WHERE `article_id` IN (
    SELECT `id` FROM `article` WHERE `source` NOT IN (SELECT `name` FROM `article_source`));

DELETE FROM `article_ingestion_job`
WHERE `article_id` IN (
    SELECT `id` FROM `article` WHERE `source` NOT IN (SELECT `name` FROM `article_source`));

DELETE FROM `article`
WHERE `source` NOT IN (SELECT `name` FROM `article_source`);

ALTER TABLE `article`
    RENAME COLUMN `source_url` TO `content_url`;

ALTER TABLE `article_source`
    ADD CONSTRAINT `uk_article_source_name` UNIQUE (`name`);

ALTER TABLE `article`
    ADD CONSTRAINT `fk_article_source_name`
        FOREIGN KEY (`source`) REFERENCES `article_source` (`name`)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

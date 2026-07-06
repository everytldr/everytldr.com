ALTER TABLE `article_source`
    ADD COLUMN `license_code` varchar(32) NULL AFTER `source_type`,
    ADD COLUMN `license_version` varchar(32) NULL AFTER `license_code`;

-- Existing sources default to UNKNOWN first so the new required license_code column can be
-- introduced safely even if a source has not been reviewed yet.
UPDATE `article_source`
SET `license_code` = 'UNKNOWN',
    `license_version` = NULL;

-- Existing reviewed CC-BY sources. These updates belong in V21 because existing article
-- snapshots are copied from article_source before license_code is made NOT NULL.
UPDATE `article_source`
SET `license_code` = 'CC-BY',
    `license_version` = '3.0'
WHERE `name` = 'Global Voices';

UPDATE `article_source`
SET `license_code` = 'CC-BY',
    `license_version` = '4.0'
WHERE `name` = '360info';

ALTER TABLE `article_source`
    MODIFY COLUMN `license_code` varchar(32) NOT NULL;

ALTER TABLE `article`
    ADD COLUMN `license_code` varchar(32) NULL AFTER `published_at`,
    ADD COLUMN `license_version` varchar(32) NULL AFTER `license_code`;

-- Preserve the source license that was current when each already-collected article is backfilled.
UPDATE `article` a
    JOIN `article_source` s ON a.`source` = s.`name`
SET a.`license_code` = s.`license_code`,
    a.`license_version` = s.`license_version`;

UPDATE `article`
SET `license_code` = 'UNKNOWN',
    `license_version` = NULL
WHERE `license_code` IS NULL;

ALTER TABLE `article`
    MODIFY COLUMN `license_code` varchar(32) NOT NULL;

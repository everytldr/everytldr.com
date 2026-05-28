ALTER TABLE `article_comment`
  ADD COLUMN `masked_ip` varchar(32) NULL;

UPDATE `article_comment`
SET `masked_ip` = 'unknown'
WHERE `masked_ip` IS NULL;

ALTER TABLE `article_comment`
  MODIFY COLUMN `masked_ip` varchar(32) NOT NULL;

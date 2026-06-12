ALTER TABLE `article_source`
    ADD COLUMN `policy` json NULL;

UPDATE `article_source`
SET `policy` = '{"crawling": {"hosts": ["globalvoices.org", "www.globalvoices.org"], "selectors": [".full-article .entry", ".post .entry", ".entry-container .entry"]}}'
WHERE `id` = 45660871069790211;

ALTER TABLE `article_source`
    MODIFY COLUMN `policy` json NOT NULL;

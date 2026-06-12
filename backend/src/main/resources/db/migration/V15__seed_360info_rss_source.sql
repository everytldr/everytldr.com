-- Fixed Snowflake-format seed ID:
-- timestamp=2026-05-07T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=4.
INSERT INTO `article_source` (`id`,
                              `updated_at`,
                              `is_active`,
                              `language`,
                              `name`,
                              `url`,
                              `policy`,
                              `source_type`)
VALUES (45660871069790212,
        '2026-05-07 00:00:00.000000',
        TRUE,
        'en',
        '360info',
        'https://360info.org/feed/',
        '{"crawling": {"hosts": ["360info.org", "www.360info.org"], "selectors": ["article.article .content-wrapper", "article.article"]}}',
        'RSS');

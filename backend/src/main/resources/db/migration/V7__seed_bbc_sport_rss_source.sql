-- Fixed Snowflake-format seed ID:
-- timestamp=2026-05-07T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=2.
INSERT INTO `article_source` (`id`,
                              `updated_at`,
                              `is_active`,
                              `language`,
                              `name`,
                              `url`,
                              `source_type`)
VALUES (45660871069790210,
        '2026-05-07 00:00:00.000000',
        TRUE,
        'en',
        'BBC Sport',
        'http://newsrss.bbc.co.uk/rss/sportonline_uk_edition/football/rss.xml',
        'RSS');

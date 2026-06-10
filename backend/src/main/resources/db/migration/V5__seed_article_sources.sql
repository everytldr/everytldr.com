-- Fixed Snowflake-format seed ID:
-- timestamp=2026-05-07T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=1.
INSERT INTO `article_source` (`id`,
                              `updated_at`,
                              `is_active`,
                              `language`,
                              `name`,
                              `url`,
                              `source_type`)
VALUES (45660871069790209,
        '2026-05-07 00:00:00.000000',
        TRUE,
        'en',
        'The Guardian Football',
        'https://content.guardianapis.com/search?section=football',
        'GUARDIAN_API');

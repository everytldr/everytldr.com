INSERT INTO `article_source` (`id`,
                              `updated_at`,
                              `is_active`,
                              `language`,
                              `name`,
                              `policy`,
                              `source_type`,
                              `license_code`,
                              `license_version`)
VALUES
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-06-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=0.
    (62693100752990208,
     '2026-06-23 00:00:00.000000',
     TRUE,
     'en',
     'Universe Today',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.universetoday.com/rss.xml'),
             'hosts', JSON_ARRAY('universetoday.com', 'www.universetoday.com'),
             'content_selectors', JSON_ARRAY('.article-content'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-06-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=1.
    (62693100752990209,
     '2026-06-23 00:00:00.000000',
     TRUE,
     'en',
     'EFF Deeplinks',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.eff.org/rss/updates.xml'),
             'hosts', JSON_ARRAY('www.eff.org'),
             'content_selectors', JSON_ARRAY('.node--full .field--name-body'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-06-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=2.
    (62693100752990210,
     '2026-06-23 00:00:00.000000',
     TRUE,
     'en',
     'APC',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.apc.org/en/rss.xml'),
             'hosts', JSON_ARRAY('www.apc.org'),
             'content_selectors', JSON_ARRAY('.field--name-body'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-06-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=3.
    (62693100752990211,
     '2026-06-23 00:00:00.000000',
     TRUE,
     'en',
     'Horizon Magazine',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls',
             JSON_ARRAY(
                 'https://projects.research-and-innovation.ec.europa.eu/horizon-magazine/articles.xml'),
             'hosts', JSON_ARRAY('projects.research-and-innovation.ec.europa.eu'),
             'content_selectors', JSON_ARRAY('.article--body'),
             'allowed_path_prefixes', JSON_ARRAY('/en/horizon-magazine/'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-06-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=4.
    (62693100752990212,
     '2026-06-23 00:00:00.000000',
     TRUE,
     'en',
     'SciDev.Net',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.scidev.net/global/rss.xml/?type=header'),
             'hosts', JSON_ARRAY('www.scidev.net'),
             'content_selectors', JSON_ARRAY('.fl-module-fl-post-content .fl-module-content'),
             'allowed_path_prefixes',
             JSON_ARRAY(
                 '/global/news/',
                 '/global/features/',
                 '/global/opinions/',
                 '/global/scidev-net-investigates/'))),
     'RSS',
     'CC-BY',
     '2.0');

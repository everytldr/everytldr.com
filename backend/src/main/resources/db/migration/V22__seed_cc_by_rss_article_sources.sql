-- CC-BY candidates deferred from this RSS seed:
-- Horizon Magazine, SciDev.Net, and Prensa Obrera need follow-up source-client or feed work.
--
-- Non-CC-BY candidates deferred for later policy support:
-- El Salto (CC BY-SA), La Marea (CC BY-SA with AI-training restriction notice),
-- elDiario.es / Spin (CC BY-NC), Tercera Informacion (CC BY-NC),
-- and Voxeurop (official site blocked automated verification; likely CC BY-NC-SA).
--
-- Thumbnail reuse policy is intentionally deferred. The current source seed only records
-- text license identity; thumbnail-specific license verification should be added separately.

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
     '4.0');

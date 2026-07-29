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
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=0.
    (73564736720990208,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'en',
     'Pressenza',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.pressenza.com/feed/'),
             'hosts', JSON_ARRAY('www.pressenza.com', 'pressenza.com'),
             'content_selectors', JSON_ARRAY('.post-content'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=1.
    (73564736720990209,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'en',
     'Africa Is a Country',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://africasacountry.com/feed'),
             'hosts', JSON_ARRAY('africasacountry.com', 'www.africasacountry.com'),
             'content_selectors', JSON_ARRAY('.po-bo__content'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=2.
    (73564736720990210,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'en',
     'NASA',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.nasa.gov/rss/dyn/breaking_news.rss'),
             'hosts', JSON_ARRAY('www.nasa.gov', 'nasa.gov', 'science.nasa.gov'),
             'content_selectors', JSON_ARRAY('.entry-content'))),
     'RSS',
     'PUBLIC_DOMAIN',
     NULL),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=3.
    (73564736720990211,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'en',
     'Our World in Data',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://ourworldindata.org/atom.xml'),
             'hosts', JSON_ARRAY('ourworldindata.org', 'www.ourworldindata.org'),
             'content_selectors', JSON_ARRAY('.centered-article-container'))),
     'RSS',
     'CC-BY',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=4.
    (73564736720990212,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'de',
     'netzpolitik.org',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://netzpolitik.org/feed/'),
             'hosts', JSON_ARRAY('netzpolitik.org', 'www.netzpolitik.org'),
             'content_selectors', JSON_ARRAY('.entry-content'))),
     'RSS',
     'CC-BY-NC-SA',
     '4.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=5.
    (73564736720990213,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'es',
     'El Salto',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.elsaltodiario.com/general/feed'),
             'hosts', JSON_ARRAY('www.elsaltodiario.com', 'elsaltodiario.com'),
             'content_selectors', JSON_ARRAY('.articulo-texto-contenido'))),
     'RSS',
     'CC-BY-SA',
     '3.0'),
    -- Fixed Snowflake-format seed ID:
    -- timestamp=2026-07-23T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=6.
    (73564736720990214,
     '2026-07-23 00:00:00.000000',
     TRUE,
     'es',
     'La Marea',
     JSON_OBJECT(
         'crawling',
         JSON_OBJECT(
             'feed_urls', JSON_ARRAY('https://www.lamarea.com/feed/'),
             'hosts', JSON_ARRAY('www.lamarea.com', 'lamarea.com'),
             'content_selectors', JSON_ARRAY('.article-content'))),
     'RSS',
     'CC-BY-SA',
     '3.0');

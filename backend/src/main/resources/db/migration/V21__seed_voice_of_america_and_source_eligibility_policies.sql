-- Fixed Snowflake-format seed ID:
-- timestamp=2026-06-16T00:00:00Z, worker=1023 reserved for Flyway reference data, sequence=0.
-- Voice of America is seeded with the same current crawling policy shape used by other RSS sources.
-- This migration does not rewrite existing article.thumbnail_url values.
INSERT INTO `article_source` (`id`,
                              `updated_at`,
                              `is_active`,
                              `language`,
                              `name`,
                              `policy`,
                              `source_type`)
VALUES (60156385693790208,
        '2026-06-16 00:00:00.000000',
        TRUE,
        'en',
        'Voice of America',
        JSON_OBJECT(
            'crawling',
            JSON_OBJECT(
                'feed_urls', JSON_ARRAY('https://www.voanews.com/api/'),
                'hosts', JSON_ARRAY('www.voanews.com', 'voanews.com'),
                'content_selectors', JSON_ARRAY('#article-content .wsw'),
                'thumbnail_selectors', JSON_ARRAY())),
        'RSS');

UPDATE `article_source`
SET `policy` = JSON_SET(
        `policy`,
        '$.eligibility',
        JSON_OBJECT(
            'article_rules', JSON_ARRAY(
                JSON_OBJECT(
                    'type', 'SELECTOR_EXISTS',
                    'selector', '.publishing-details .links__item-link'),
                JSON_OBJECT(
                    'type', 'SELECTOR_ATTRIBUTE_PREFIX_ANY',
                    'selector', '.publishing-details .links__item-link',
                    'attribute', 'href',
                    'values',
                    JSON_ARRAY(
                        '/author/',
                        'https://www.voanews.com/author/',
                        'https://voanews.com/author/')),
                JSON_OBJECT(
                    'type', 'SELECTOR_TEXT_NOT_EQUALS_ANY',
                    'selector', '.publishing-details .links__item-link',
                    'values', JSON_ARRAY('VOA News')),
                JSON_OBJECT(
                    'type', 'DOCUMENT_HTML_NOT_CONTAINS_ANY',
                    'values',
                    JSON_ARRAY(
                        'wire service reports',
                        'Associated Press',
                        'Reuters',
                        'Agence France-Presse',
                        '(AFP)'))),
            'thumbnail_policy', 'ELIGIBLE_ONLY',
            'thumbnail_eligibility', JSON_OBJECT(
                'candidate_selectors', JSON_ARRAY(
                    JSON_OBJECT(
                        'selector', 'figure img',
                        'url_attribute', 'src',
                        'credit_container_selector', 'figure',
                        'credit_selectors', JSON_ARRAY('figcaption', '.caption'))),
                'allowed_credit_fragments', JSON_ARRAY('VOA', 'Voice of America'),
                'denied_credit_fragments',
                JSON_ARRAY(
                    'Associated Press',
                    'AP Photo',
                    'Reuters',
                    'Agence France-Presse',
                    'AFP',
                    'Getty',
                    'Used with permission'))))
WHERE `id` = 60156385693790208;

-- These existing-source thumbnail policies apply to new collection or reprocessing only.
-- RSS feed thumbnails lack reliable credit context, so ELIGIBLE_ONLY sources skip them.
UPDATE `article_source`
SET `policy` = JSON_SET(
        `policy`,
        '$.eligibility',
        JSON_OBJECT(
            'article_rules', JSON_ARRAY(),
            'thumbnail_policy', 'ELIGIBLE_ONLY',
            'thumbnail_eligibility', JSON_OBJECT(
                'candidate_selectors', JSON_ARRAY(
                    JSON_OBJECT(
                        'selector', 'figure img',
                        'url_attribute', 'src',
                        'credit_container_selector', 'figure',
                        'credit_selectors', JSON_ARRAY('figcaption', '.caption', '.wp-caption-text'))),
                'allowed_credit_fragments',
                JSON_ARRAY('Creative Commons', 'CC BY', 'Global Voices', 'Public Domain'),
                'denied_credit_fragments',
                JSON_ARRAY(
                    'Used with permission',
                    'Getty',
                    'Reuters',
                    'Associated Press',
                    'AP Photo',
                    'AFP'))))
WHERE `id` = 45660871069790211;

UPDATE `article_source`
SET `policy` = JSON_SET(
        `policy`,
        '$.eligibility',
        JSON_OBJECT(
            'article_rules', JSON_ARRAY(),
            'thumbnail_policy', 'ELIGIBLE_ONLY',
            'thumbnail_eligibility', JSON_OBJECT(
                'candidate_selectors', JSON_ARRAY(
                    JSON_OBJECT(
                        'selector', '.feat-image img',
                        'url_attribute', 'src',
                        'credit_container_selector', '.feat-image',
                        'credit_selectors', JSON_ARRAY('figcaption', '.caption', '.wp-caption-text'))),
                'allowed_credit_fragments',
                JSON_ARRAY('Creative Commons', 'CC BY', '360info', 'Public Domain'),
                'denied_credit_fragments',
                JSON_ARRAY(
                    'Getty',
                    'Reuters',
                    'Associated Press',
                    'AP Photo',
                    'AFP',
                    'used with permission',
                    'third party'))))
WHERE `id` = 45660871069790212;

-- Move the single source URL into policy.crawling.feed_urls and widen coverage
-- with category/topic feeds for Global Voices and 360info, then drop the now-unused url column.

UPDATE `article_source`
SET `policy` = JSON_SET(
        `policy`,
        '$.crawling.feed_urls',
        JSON_ARRAY(
            'https://globalvoices.org/feed/',
            'https://globalvoices.org/-/topics/politics/feed/',
            'https://globalvoices.org/-/topics/citizen-media/feed/',
            'https://globalvoices.org/-/topics/governance/feed/',
            'https://globalvoices.org/-/topics/human-rights/feed/',
            'https://globalvoices.org/-/topics/media-journalism/feed/',
            'https://globalvoices.org/-/topics/protest/feed/',
            'https://globalvoices.org/-/topics/digital-activism/feed/',
            'https://globalvoices.org/-/topics/international-relations/feed/',
            'https://globalvoices.org/-/topics/arts-culture/feed/',
            'https://globalvoices.org/-/topics/freedom-of-speech/feed/',
            'https://globalvoices.org/-/topics/law/feed/',
            'https://globalvoices.org/-/topics/technology/feed/',
            'https://globalvoices.org/-/topics/economics-business/feed/',
            'https://globalvoices.org/-/topics/war-conflict/feed/',
            'https://globalvoices.org/-/topics/history/feed/',
            'https://globalvoices.org/-/topics/environment/feed/',
            'https://globalvoices.org/-/topics/elections/feed/',
            'https://globalvoices.org/-/topics/youth/feed/',
            'https://globalvoices.org/-/topics/women-gender/feed/',
            'https://globalvoices.org/-/topics/ideas/feed/',
            'https://globalvoices.org/-/topics/development/feed/',
            'https://globalvoices.org/-/topics/ethnicity-race/feed/',
            'https://globalvoices.org/-/topics/travel/feed/',
            'https://globalvoices.org/-/topics/breaking-news/feed/',
            'https://globalvoices.org/-/topics/religion/feed/',
            'https://globalvoices.org/-/topics/education/feed/',
            'https://globalvoices.org/-/topics/migration-immigration/feed/',
            'https://globalvoices.org/-/topics/health/feed/',
            'https://globalvoices.org/-/topics/disaster/feed/',
            'https://globalvoices.org/-/topics/humor/feed/',
            'https://globalvoices.org/-/topics/music/feed/',
            'https://globalvoices.org/-/topics/humanitarian-response/feed/',
            'https://globalvoices.org/-/topics/sport/feed/',
            'https://globalvoices.org/-/topics/food/feed/',
            'https://globalvoices.org/-/topics/photography/feed/',
            'https://globalvoices.org/-/topics/literature/feed/',
            'https://globalvoices.org/-/topics/censorship/feed/',
            'https://globalvoices.org/-/topics/labor/feed/',
            'https://globalvoices.org/-/topics/language/feed/',
            'https://globalvoices.org/-/topics/film/feed/',
            'https://globalvoices.org/-/topics/indigenous/feed/',
            'https://globalvoices.org/-/topics/gay-rights-lgbt/feed/',
            'https://globalvoices.org/-/topics/refugees/feed/',
            'https://globalvoices.org/-/topics/good-news/feed/',
            'https://globalvoices.org/-/topics/science/feed/',
            'https://globalvoices.org/-/topics/animal-rights/feed/'))
WHERE `id` = 45660871069790211;

UPDATE `article_source`
SET `policy` = JSON_SET(
        `policy`,
        '$.crawling.feed_urls',
        JSON_ARRAY(
            'https://360info.org/feed/',
            'https://360info.org/category/economy/feed/',
            'https://360info.org/category/education/feed/',
            'https://360info.org/category/environment/feed/',
            'https://360info.org/category/health/feed/',
            'https://360info.org/category/politics/feed/',
            'https://360info.org/category/technology/feed/',
            'https://360info.org/category/science/feed/',
            'https://360info.org/category/society/feed/',
            'https://360info.org/category/special-report/feed/'))
WHERE `id` = 45660871069790212;

ALTER TABLE `article_source`
    DROP COLUMN `url`;

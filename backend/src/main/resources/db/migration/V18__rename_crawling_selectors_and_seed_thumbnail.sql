UPDATE `article_source`
SET `policy` = JSON_SET(
        JSON_REMOVE(`policy`, '$.crawling.selectors'),
        '$.crawling.content_selectors', JSON_EXTRACT(`policy`, '$.crawling.selectors'))
WHERE JSON_CONTAINS_PATH(`policy`, 'one', '$.crawling.selectors');

UPDATE `article_source`
SET `policy` = JSON_SET(`policy`, '$.crawling.thumbnail_selectors', JSON_ARRAY('.feat-image > img'))
WHERE `name` = '360info';
